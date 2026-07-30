package com.hezi.juyumao.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.repository.MetadataRepository
import com.hezi.juyumao.player.audio.LyricsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songDao: SongDao,
    private val metadataRepository: MetadataRepository,
) : ViewModel() {

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong

    private val _artworkUri = MutableStateFlow<String?>(null)
    val artworkUri: StateFlow<String?> = _artworkUri

    private val _lyrics = MutableStateFlow<LyricsData?>(null)
    val lyrics: StateFlow<LyricsData?> = _lyrics

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    init {
        // 从导航参数获取 songId 并加载歌曲
        val songId = savedStateHandle.get<Long>("songId")
        if (songId != null && songId > 0) {
            loadSong(songId)
        }
    }

    private fun loadSong(songId: Long) {
        viewModelScope.launch {
            val song = songDao.getById(songId)
            if (song != null) {
                _currentSong.value = song

                // 加载封面
                val artPath = metadataRepository.getCachedArtworkPath(song.id)
                _artworkUri.value = artPath

                // 如果没有缓存封面，尝试提取
                if (artPath == null && song.source == "LOCAL") {
                    try {
                        val newPath = metadataRepository.extractAndCacheArtwork(song)
                        _artworkUri.value = newPath
                    } catch (_: Exception) {}
                }

                // 加载歌词
                try {
                    _lyrics.value = metadataRepository.getLyrics(song)
                } catch (_: Exception) {}
            }
        }
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }
}
