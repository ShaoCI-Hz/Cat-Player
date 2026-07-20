package com.example.smbplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smbplayer.data.local.AlbumEntry
import com.example.smbplayer.data.local.ArtistEntry
import com.example.smbplayer.data.local.LocalMusicRepository
import com.example.smbplayer.data.local.LocalTrack
import com.example.smbplayer.data.smb.ConnectionState
import com.example.smbplayer.data.smb.SmbConnectionManager
import com.example.smbplayer.data.smb.SmbFileBrowser
import com.example.smbplayer.data.smb.SmbFileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistEntry(val name: String, val trackCount: Int, val albums: List<AlbumEntry>)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localMusicRepository: LocalMusicRepository,
    private val connectionManager: SmbConnectionManager,
    private val fileBrowser: SmbFileBrowser
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    private val _localTracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val localTracks: StateFlow<List<LocalTrack>> = _localTracks.asStateFlow()

    private val _artists = MutableStateFlow<List<com.example.smbplayer.data.local.ArtistEntry>>(emptyList())
    private val _albums = MutableStateFlow<List<AlbumEntry>>(emptyList())
    val albums: StateFlow<List<AlbumEntry>> = _albums.asStateFlow()

    val artists: StateFlow<List<ArtistEntry>> = _artists.asStateFlow()
    val totalArtistCount get() = _artists.value.size
    val totalAlbumCount get() = _albums.value.size

    private val _smbEntries = MutableStateFlow<List<SmbFileEntry>>(emptyList())
    val smbEntries: StateFlow<List<SmbFileEntry>> = _smbEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPath = MutableStateFlow(".")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _breadcrumbs = MutableStateFlow(listOf("."))
    val breadcrumbs: StateFlow<List<String>> = _breadcrumbs.asStateFlow()

    private var loadJob: Job? = null
    private var smbLoadJob: Job? = null

    fun loadLocalTracks() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val tracks = localMusicRepository.loadAllTracks()
                _localTracks.value = tracks
                _albums.value = tracks
                    .groupBy { it.album.ifEmpty { "未知专辑" } }
                    .map { (name, group) -> AlbumEntry(name, group.first().artist, group) }
                    .sortedBy { it.name }
                _artists.value = tracks
                    .groupBy { it.artist.ifEmpty { "Unknown" } }
                    .map { (name, group) -> com.example.smbplayer.data.local.ArtistEntry(name, group) }
                    .sortedBy { it.name }
            } finally { _isLoading.value = false }
        }
    }

    fun loadSMBDirectory(path: String = _currentPath.value) {
        if (connectionState.value != ConnectionState.Connected) return
        smbLoadJob?.cancel()
        smbLoadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                _smbEntries.value = fileBrowser.listFiles(path)
                _currentPath.value = path
                _breadcrumbs.value = if (path == ".") listOf(".")
                    else listOf(".") + path.split("/").filter { it.isNotEmpty() }
            } finally { _isLoading.value = false }
        }
    }

    fun navigateInto(entry: SmbFileEntry) { if (entry.isDirectory) loadSMBDirectory(entry.path) }
    fun navigateToBreadcrumb(index: Int) {
        val crumbs = _breadcrumbs.value
        if (index < crumbs.size) loadSMBDirectory(if (index == 0) "." else crumbs.subList(1, index + 1).joinToString("/"))
    }
}
