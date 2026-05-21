package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DuplicateGroup
import com.example.data.PhotoCleanerRepository
import com.example.data.ServiceLocator
import com.example.data.database.PhotoEmbedding
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Unscanned : ScanUiState
    data class Scanning(val progress: Int) : ScanUiState
    object Scanned : ScanUiState
}

class CleanerViewModel(
    private val repository: PhotoCleanerRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Unscanned)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    // Observe all raw embeddings from the DB
    val allPhotos: StateFlow<List<PhotoEmbedding>> = repository.allEmbeddings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        // Pre-populate selections automatically as soon as duplicates update
        viewModelScope.launch {
            duplicateGroups.collect { groups ->
                // By default, select all unrecommended duplicate copy versions (keeping only the keeper)
                val autoSelects = groups.flatMap { g -> g.duplicates.map { d -> d.uri } }.toSet()
                _selectedUris.value = autoSelects
            }
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
            onComplete()
        }
    }

    fun deleteSinglePhoto(uri: String) {
        viewModelScope.launch {
            repository.deletePhotos(listOf(uri))
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CleanerViewModel(ServiceLocator.photoCleanerRepository) as T
            }
        }
    }
}
