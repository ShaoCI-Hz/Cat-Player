package com.hezi.juyumao.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hezi.juyumao.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateHolder @Inject constructor() {

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong

    private val _artworkUri = MutableStateFlow<String?>(null)
    val artworkUri: StateFlow<String?> = _artworkUri

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private var exoPlayer: ExoPlayer? = null

    fun bindPlayer(player: ExoPlayer) {
        exoPlayer = player
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0)
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }
        })
    }

    fun updateSong(song: SongEntity?) {
        _currentSong.value = song
    }

    fun updateArtwork(uri: String?) {
        _artworkUri.value = uri
    }

    fun updatePlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _position.value = positionMs
    }

    fun pollPosition() {
        exoPlayer?.let {
            if (it.isPlaying) {
                _position.value = it.currentPosition
                _duration.value = it.duration.coerceAtLeast(0)
            }
        }
    }
}
