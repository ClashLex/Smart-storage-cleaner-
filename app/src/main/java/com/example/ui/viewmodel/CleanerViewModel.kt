package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DuplicateGroup
import com.example.data.PhotoCleanerRepository
import com.example.data.ServiceLocator
import com.example.data.StorageStats
import com.example.data.StorageStatsRepository
import com.example.data.database.PhotoEmbedding
import com.example.domain.JunkItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Unscanned : ScanUiState
    data class Scanning(val progress: Int) : ScanUiState
    object Scanned : ScanUiState
}

class CleanerViewModel(
    private val repository: PhotoCleanerRepository,
    private val storageStatsRepository: StorageStatsRepository
) : ViewModel() {

    private val _storageStats = MutableStateFlow<StorageStats>(StorageStats(0L, 0L, 0L, 0f))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Unscanned)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    // Observe all raw embeddings from the DB
    val allPhotos: StateFlow<List<PhotoEmbedding>> = repository.allEmbeddings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Non-photo junk tracks (WhatsApp, Old APKs, App Cache)
    private val _whatsappItems = MutableStateFlow<List<JunkItem>>(generateMockWhatsAppItems())
    val whatsappItems: StateFlow<List<JunkItem>> = _whatsappItems.asStateFlow()

    private val _apkItems = MutableStateFlow<List<JunkItem>>(generateMockApkItems())
    val apkItems: StateFlow<List<JunkItem>> = _apkItems.asStateFlow()

    private val _cacheItems = MutableStateFlow<List<JunkItem>>(generateMockCacheItems())
    val cacheItems: StateFlow<List<JunkItem>> = _cacheItems.asStateFlow()

    private val _totalReclaimedBytes = MutableStateFlow<Long>(0L)
    val totalReclaimedBytes: StateFlow<Long> = _totalReclaimedBytes.asStateFlow()

    // Derived StateFlow: Automatically cluster duplicates reactively whenever the DB changes
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = allPhotos
        .map { photos -> repository.groupDuplicates(photos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived StateFlow: Filter blurry pictures out of the raw photo listings
    val blurryPhotos: StateFlow<List<PhotoEmbedding>> = allPhotos
        .map { photos -> photos.filter { it.blurScore < 15.0 } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User-selected duplicates to delete (holds file URIs)
    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    init {
        // Fetch storage statistics initially
        refreshStorageStats()

        // Pre-populate selections automatically as soon as duplicates update
        viewModelScope.launch {
            duplicateGroups.collect { groups ->
                // By default, select all unrecommended duplicate copy versions (keeping only the keeper)
                val autoSelects = groups.flatMap { g -> g.duplicates.map { d -> d.uri } }.toSet()
                _selectedUris.value = autoSelects
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = storageStatsRepository.getStorageStats()
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning(0)
            _selectedUris.value = emptySet()
            repository.performSmartScan { progress ->
                _scanState.value = ScanUiState.Scanning(progress)
            }
            _scanState.value = ScanUiState.Scanned
            refreshStorageStats()
        }
    }

    fun toggleSelection(uri: String) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedUris.value = current
    }

    fun selectAll() {
        // Collect all duplicates items
        val allDuplicates = duplicateGroups.value.flatMap { group -> group.duplicates.map { d -> d.uri } }.toSet()
        _selectedUris.value = allDuplicates
    }

    fun selectNone() {
        _selectedUris.value = emptySet()
    }

    fun deleteSelectedDuplicates(onComplete: () -> Unit = {}) {
        val toDelete = _selectedUris.value.toList()
        viewModelScope.launch {
            repository.deletePhotos(toDelete)
            _selectedUris.value = emptySet()
            refreshStorageStats()
            onComplete()
        }
    }

    fun deleteSinglePhoto(uri: String) {
        viewModelScope.launch {
            repository.deletePhotos(listOf(uri))
            refreshStorageStats()
        }
    }

    fun toggleWhatsAppItem(id: String) {
        _whatsappItems.value = _whatsappItems.value.map {
            if (it.id == id) it.copy(checked = !it.checked) else it
        }
    }

    fun toggleApkItem(id: String) {
        _apkItems.value = _apkItems.value.map {
            if (it.id == id) it.copy(checked = !it.checked) else it
        }
    }

    fun toggleCacheItem(id: String) {
        _cacheItems.value = _cacheItems.value.map {
            if (it.id == id) it.copy(checked = !it.checked) else it
        }
    }

    fun selectAllJunk(category: String, selected: Boolean) {
        when (category) {
            "WhatsApp" -> {
                _whatsappItems.value = _whatsappItems.value.map { it.copy(checked = selected) }
            }
            "Old APKs" -> {
                _apkItems.value = _apkItems.value.map { it.copy(checked = selected) }
            }
            "App Cache" -> {
                _cacheItems.value = _cacheItems.value.map { it.copy(checked = selected) }
            }
        }
    }

    fun deleteWhatsAppItems(ids: List<String>) {
        val currentList = _whatsappItems.value
        val itemsToDelete = currentList.filter { it.id in ids }
        val sizeFreed = itemsToDelete.sumOf { it.size }
        _whatsappItems.value = currentList.filter { it.id !in ids }
        _totalReclaimedBytes.value += sizeFreed
        refreshStorageStats()
    }

    fun deleteApkItems(ids: List<String>) {
        val currentList = _apkItems.value
        val itemsToDelete = currentList.filter { it.id in ids }
        val sizeFreed = itemsToDelete.sumOf { it.size }
        _apkItems.value = currentList.filter { it.id !in ids }
        _totalReclaimedBytes.value += sizeFreed
        refreshStorageStats()
    }

    fun deleteCacheItems(ids: List<String>) {
        val currentList = _cacheItems.value
        val itemsToDelete = currentList.filter { it.id in ids }
        val sizeFreed = itemsToDelete.sumOf { it.size }
        _cacheItems.value = currentList.filter { it.id !in ids }
        _totalReclaimedBytes.value += sizeFreed
        refreshStorageStats()
    }

    private fun generateMockWhatsAppItems(): List<JunkItem> {
        return listOf(
            JunkItem("wa_1", "VID_20260515_182041.mp4", 1280000000L, "WhatsApp Video / Shared Multiple Times", "May 15, 2026"),
            JunkItem("wa_2", "VID_20260422_WA0012.mp4", 980000000L, "WhatsApp Video / Forwarded Category", "Apr 22, 2026"),
            JunkItem("wa_3", "IMG_Meme_Cat_Lovers.jpg", 18800000L, "WhatsApp Image / Meme Library", "May 20, 2026"),
            JunkItem("wa_4", "WA_Audio_Note_781.opus", 15400000L, "WhatsApp Audio / Voice Note Cache", "May 19, 2026"),
            JunkItem("wa_5", "WhatsApp_Backup_Stale.zip", 2215822000L, "WhatsApp Document / Stale Chat Backup", "Jan 12, 2026")
        )
    }

    private fun generateMockApkItems(): List<JunkItem> {
        return listOf(
            JunkItem("apk_1", "com.whatsapp.android_2.26.15.apk", 820000000L, "Stale Installer / Download Directory", "May 12, 2014"),
            JunkItem("apk_2", "facebook_lite_new.apk", 430000000L, "Local Dev Build / Leftover package", "May 01, 2026"),
            JunkItem("apk_3", "temp_unsigned_debug.apk", 300000000L, "Android Gradle Build / Leftover debug build", "Apr 28, 2026")
        )
    }

    private fun generateMockCacheItems(): List<JunkItem> {
        return listOf(
            JunkItem("cache_1", "Google Chrome Cache", 310000000L, "Cached image previews, scripts & styles", "Just now"),
            JunkItem("cache_2", "YouTube Offline Buffer", 250000000L, "Temporary streaming block and media segments", "Just now"),
            JunkItem("cache_3", "Spotify Music Artwork", 180000000L, "Cached PNG image albums / cover files", "Just now"),
            JunkItem("cache_4", "Instagram Stories Preload", 100000000L, "Pre-fetched video stories and feed images", "Just now")
        )
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CleanerViewModel(
                    repository = ServiceLocator.photoCleanerRepository,
                    storageStatsRepository = ServiceLocator.storageStatsRepository
                ) as T
            }
        }
    }
}
