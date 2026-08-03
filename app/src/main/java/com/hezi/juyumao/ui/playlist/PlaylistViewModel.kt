package com.hezi.juyumao.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.db.dao.PlaylistDao
import com.hezi.juyumao.data.local.db.entity.PlaylistEntity
import com.hezi.juyumao.data.local.db.entity.PlaylistSongEntity
import com.hezi.juyumao.data.local.db.entity.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
) : ViewModel() {

    val playlists: StateFlow<List<com.hezi.juyumao.data.local.db.dao.PlaylistWithCount>> =
        playlistDao.getPlaylistsWithCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前打开的歌单详情 */
    private val _currentPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val currentPlaylist: StateFlow<PlaylistEntity?> = _currentPlaylist

    private val _currentSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val currentSongs: StateFlow<List<SongEntity>> = _currentSongs

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistDao.insert(PlaylistEntity(name = name.trim()))
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistDao.rename(id, name.trim())
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistDao.delete(id)
            if (_currentPlaylist.value?.id == id) {
                _currentPlaylist.value = null
                _currentSongs.value = emptyList()
            }
        }
    }

    /** 打开歌单详情（快照式加载歌曲列表） */
    fun openPlaylist(id: Long) {
        viewModelScope.launch {
            val playlist = playlistDao.getById(id) ?: return@launch
            _currentPlaylist.value = playlist
            _currentSongs.value = playlistDao.getPlaylistSongsOnce(id)
        }
    }

    fun closePlaylist() {
        _currentPlaylist.value = null
        _currentSongs.value = emptyList()
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            if (playlistDao.isSongInPlaylist(playlistId, songId) == null) {
                playlistDao.addSong(PlaylistSongEntity(playlistId, songId))
            }
        }
    }

    fun removeSong(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistDao.removeSong(playlistId, songId)
            // 刷新当前列表
            _currentSongs.value = playlistDao.getPlaylistSongsOnce(playlistId)
        }
    }
}
