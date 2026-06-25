package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DuplicateGroup
import com.example.data.JunkScanRepository
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
    private val storageStatsRepository: StorageStatsRepository,
    private val junkScanRepository: JunkScanRepository,
    private val context: Context
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
    private val _whatsappItems = MutableStateFlow<List<JunkItem>>(emptyList())
    val whatsappItems: StateFlow<List<JunkItem>> = _whatsappItems.asStateFlow()

    private val _apkItems = MutableStateFlow<List<JunkItem>>(emptyList())
    val apkItems: StateFlow<List<JunkItem>> = _apkItems.asStateFlow()

    private val _cacheItems = MutableStateFlow<List<JunkItem>>(emptyList())
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
        viewModelScope.launch {
            loadRealJunkData()
        }

        // Pre-populate selections automatically as soon as duplicates update
        viewModelScope.launch {
            duplicateGroups.collect { groups ->
                // By default, select all unrecommended duplicate copy versions (keeping only the keeper)
                val autoSelects = groups.flatMap { g -> g.duplicates.map { d -> d.uri } }.toSet()
                _selectedUris.value = autoSelects
            }
        }
    }

    suspend fun loadRealJunkData() {
        val result = junkScanRepository.performFullScan(context)
        _whatsappItems.value = result.whatsappItems
        _apkItems.value = result.apkItems
        _cacheItems.value = result.cacheItems
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

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CleanerViewModel(
                    repository = ServiceLocator.photoCleanerRepository,
                    storageStatsRepository = ServiceLocator.storageStatsRepository,
                    junkScanRepository = ServiceLocator.junkScanRepository,
                    context = ServiceLocator.appContext
                ) as T
            }
        }
    }
}
