package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "smart_cleaner_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val SUB_EXPIRY = longPreferencesKey("sub_expiry")

        val CLEANUP_FREQUENCY = stringPreferencesKey("cleanup_frequency")
        val AUTO_CLEAN_CACHE = booleanPreferencesKey("auto_clean_cache")
        val AUTO_CLEAN_APKS = booleanPreferencesKey("auto_clean_apks")
        val SCAN_PHOTOS_AI = booleanPreferencesKey("scan_photos_ai")
        val SCAN_BLURRY_PHOTOS = booleanPreferencesKey("scan_blurry_photos")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CHARGING_ONLY = booleanPreferencesKey("charging_only")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val CRITICAL_STORAGE_THRESHOLD = intPreferencesKey("critical_storage_threshold")
    }

    data class UserSession(
        val userId: String?,
        val name: String?,
        val email: String?,
        val token: String?,
        val isPremium: Boolean,
        val expiryTime: Long
    )

    data class CleanerSettings(
        val cleanupFrequency: String, // "Daily", "Weekly", "Monthly", "Never"
        val autoCleanCache: Boolean,
        val autoCleanApks: Boolean,
        val scanPhotosAi: Boolean,
        val scanBlurryPhotos: Boolean,
        val wifiOnly: Boolean,
        val chargingOnly: Boolean,
        val notificationsEnabled: Boolean,
        val quietHoursStart: Int, // 0-23
        val quietHoursEnd: Int, // 0-23
        val criticalStorageThreshold: Int // Percentage
    )

    val userSession: Flow<UserSession> = context.dataStore.data
        .map { preferences ->
            UserSession(
                userId = preferences[PreferencesKeys.USER_ID],
                name = preferences[PreferencesKeys.USER_NAME],
                email = preferences[PreferencesKeys.USER_EMAIL],
                token = preferences[PreferencesKeys.AUTH_TOKEN],
                isPremium = preferences[PreferencesKeys.IS_PREMIUM] ?: false,
                expiryTime = preferences[PreferencesKeys.SUB_EXPIRY] ?: 0L
            )
        }

    val cleanerSettings: Flow<CleanerSettings> = context.dataStore.data
        .map { preferences ->
            CleanerSettings(
                cleanupFrequency = preferences[PreferencesKeys.CLEANUP_FREQUENCY] ?: "Weekly",
                autoCleanCache = preferences[PreferencesKeys.AUTO_CLEAN_CACHE] ?: true,
                autoCleanApks = preferences[PreferencesKeys.AUTO_CLEAN_APKS] ?: true,
                scanPhotosAi = preferences[PreferencesKeys.SCAN_PHOTOS_AI] ?: true,
                scanBlurryPhotos = preferences[PreferencesKeys.SCAN_BLURRY_PHOTOS] ?: false, // Pro feature
                wifiOnly = preferences[PreferencesKeys.WIFI_ONLY] ?: true,
                chargingOnly = preferences[PreferencesKeys.CHARGING_ONLY] ?: false,
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                quietHoursStart = preferences[PreferencesKeys.QUIET_HOURS_START] ?: 22, // 10 PM
                quietHoursEnd = preferences[PreferencesKeys.QUIET_HOURS_END] ?: 8, // 8 AM
                criticalStorageThreshold = preferences[PreferencesKeys.CRITICAL_STORAGE_THRESHOLD] ?: 85
            )
        }

    suspend fun saveUserSession(userId: String, name: String, email: String, token: String, isPremium: Boolean, expiryTime: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.USER_NAME] = name
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.AUTH_TOKEN] = token
            preferences[PreferencesKeys.IS_PREMIUM] = isPremium
            preferences[PreferencesKeys.SUB_EXPIRY] = expiryTime
        }
    }

    suspend fun updatePremiumStatus(isPremium: Boolean, expiryTime: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM] = isPremium
            preferences[PreferencesKeys.SUB_EXPIRY] = expiryTime
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.USER_NAME)
            preferences.remove(PreferencesKeys.USER_EMAIL)
            preferences.remove(PreferencesKeys.AUTH_TOKEN)
            preferences.remove(PreferencesKeys.IS_PREMIUM)
            preferences.remove(PreferencesKeys.SUB_EXPIRY)
        }
    }

    suspend fun updateCleanupSettings(
        cleanupFrequency: String,
        autoCleanCache: Boolean,
        autoCleanApks: Boolean,
        scanPhotosAi: Boolean,
        scanBlurryPhotos: Boolean,
        wifiOnly: Boolean,
        chargingOnly: Boolean,
        notificationsEnabled: Boolean,
        quietHoursStart: Int,
        quietHoursEnd: Int,
        criticalStorageThreshold: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLEANUP_FREQUENCY] = cleanupFrequency
            preferences[PreferencesKeys.AUTO_CLEAN_CACHE] = autoCleanCache
            preferences[PreferencesKeys.AUTO_CLEAN_APKS] = autoCleanApks
            preferences[PreferencesKeys.SCAN_PHOTOS_AI] = scanPhotosAi
            preferences[PreferencesKeys.SCAN_BLURRY_PHOTOS] = scanBlurryPhotos
            preferences[PreferencesKeys.WIFI_ONLY] = wifiOnly
            preferences[PreferencesKeys.CHARGING_ONLY] = chargingOnly
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = notificationsEnabled
            preferences[PreferencesKeys.QUIET_HOURS_START] = quietHoursStart
            preferences[PreferencesKeys.QUIET_HOURS_END] = quietHoursEnd
            preferences[PreferencesKeys.CRITICAL_STORAGE_THRESHOLD] = criticalStorageThreshold
        }
    }
}
