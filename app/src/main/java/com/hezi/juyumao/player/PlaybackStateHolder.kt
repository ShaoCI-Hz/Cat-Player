package com.hezi.juyumao.player

import com.hezi.juyumao.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局播放状态持有者
 * PlayerViewModel 写入，MiniPlayerBar 读取
 */
@Singleton
class PlaybackStateHolder @Inject constructor() {

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong

    private val _artworkUri = MutableStateFlow<String?>(null)
    val artworkUri: StateFlow<String?> = _artworkUri

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun updateSong(song: SongEntity?) {
        _currentSong.value = song
    }

    fun updateArtwork(uri: String?) {
        _artworkUri.value = uri
    }

    fun updatePlaying(playing: Boolean) {
        _isPlaying.value = playing
    }
}
