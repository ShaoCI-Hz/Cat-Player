package com.hezi.juyumao.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun setSong(song: SongEntity) {
        _currentSong.value = song
        viewModelScope.launch {
            // 提取封面
            val artPath = metadataRepository.getCachedArtworkPath(song.id)
                ?: metadataRepository.extractAndCacheArtwork(song)
            _artworkUri.value = artPath

            // 提取歌词
            _lyrics.value = metadataRepository.getLyrics(song)
        }
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }
}
