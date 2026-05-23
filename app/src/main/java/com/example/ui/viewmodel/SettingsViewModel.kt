package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ServiceLocator
import com.example.data.UserPreferencesRepository
import com.example.data.UserPreferencesRepository.CleanerSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val settings: StateFlow<CleanerSettings> = preferencesRepository.cleanerSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CleanerSettings(
                cleanupFrequency = "Weekly",
                autoCleanCache = true,
                autoCleanApks = true,
                scanPhotosAi = true,
                scanBlurryPhotos = false,
                wifiOnly = true,
                chargingOnly = false,
                notificationsEnabled = true,
                quietHoursStart = 22,
                quietHoursEnd = 8,
                criticalStorageThreshold = 85,
                scheduledCleanupEnabled = true
            )
        )

    fun updateSettings(updated: CleanerSettings) {
        viewModelScope.launch {
            preferencesRepository.updateCleanupSettings(
                cleanupFrequency = updated.cleanupFrequency,
                autoCleanCache = updated.autoCleanCache,
                autoCleanApks = updated.autoCleanApks,
                scanPhotosAi = updated.scanPhotosAi,
                scanBlurryPhotos = updated.scanBlurryPhotos,
                wifiOnly = updated.wifiOnly,
                chargingOnly = updated.chargingOnly,
                notificationsEnabled = updated.notificationsEnabled,
                quietHoursStart = updated.quietHoursStart,
                quietHoursEnd = updated.quietHoursEnd,
                criticalStorageThreshold = updated.criticalStorageThreshold,
                scheduledCleanupEnabled = updated.scheduledCleanupEnabled
            )
        }
    }

    fun toggleScheduledCleanup(context: android.content.Context, enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(scheduledCleanupEnabled = enabled))
        if (enabled) {
            com.example.work.AutoCleanWorker.schedule(context.applicationContext)
        } else {
            com.example.work.AutoCleanWorker.cancel(context.applicationContext)
        }
    }

    fun setCleanupFrequency(frequency: String) {
        val current = settings.value
        updateSettings(current.copy(cleanupFrequency = frequency))
    }

    fun toggleAutoCleanCache(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(autoCleanCache = enabled))
    }

    fun toggleAutoCleanApks(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(autoCleanApks = enabled))
    }

    fun toggleScanPhotosAi(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(scanPhotosAi = enabled))
    }

    fun toggleScanBlurryPhotos(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(scanBlurryPhotos = enabled))
    }

    fun toggleWifiOnly(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(wifiOnly = enabled))
    }

    fun toggleChargingOnly(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(chargingOnly = enabled))
    }

    fun toggleNotificationsEnabled(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(notificationsEnabled = enabled))
    }

    fun setQuietHours(start: Int, end: Int) {
        val current = settings.value
        updateSettings(current.copy(quietHoursStart = start, quietHoursEnd = end))
    }

    fun setCriticalStorageThreshold(threshold: Int) {
        val current = settings.value
        updateSettings(current.copy(criticalStorageThreshold = threshold))
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(ServiceLocator.userPreferencesRepository) as T
            }
        }
    }
}
