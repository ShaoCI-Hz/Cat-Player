package com.example.smbplayer.data.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.smbplayer.data.smb.SmbDataSource
import com.example.smbplayer.data.smb.SmbFileBrowser
import com.example.smbplayer.data.audio.AudioEffectManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smbFileBrowser: SmbFileBrowser,
    private val audioEffectManager: AudioEffectManager
) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    50_000,  // minBufferMs
                    100_000, // maxBufferMs
                    10_000,  // bufferForPlaybackMs
                    15_000   // bufferForPlaybackAfterRebufferMs
                )
                .setTargetBufferBytes(10 * 1024 * 1024) // 10MB
                .build()
        )
        .build()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var currentTrack: TrackInfo? = null
    private var positionJob: Job? = null
    private var isReleased = false

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _duration.value = exoPlayer.duration
                        audioEffectManager.attach(exoPlayer.audioSessionId)
                        if (exoPlayer.playWhenReady) {
                            startPositionUpdates()
                            val track = currentTrack
                            if (track != null) {
                                _playerState.value = PlayerState.Playing(track)
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        stopPositionUpdates()
                        _playerState.value = PlayerState.Idle
                    }
                    Player.STATE_BUFFERING -> {
                        _playerState.value = PlayerState.Loading
                    }
                    Player.STATE_IDLE -> {
                        stopPositionUpdates()
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                val track = currentTrack
                if (track != null) {
                    if (playWhenReady) {
                        startPositionUpdates()
                        _playerState.value = PlayerState.Playing(track)
                    } else {
                        stopPositionUpdates()
                        _playerState.value = PlayerState.Paused(track)
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                stopPositionUpdates()
                _playerState.value = PlayerState.Error(
                    currentTrack,
                    error.localizedMessage ?: "播放出错"
                )
            }
        })
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _currentPosition.value = exoPlayer.currentPosition
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    fun getExoPlayer(): ExoPlayer = exoPlayer

    fun prepare(track: TrackInfo, startPositionMs: Long = 0) {
        currentTrack = track
        _playerState.value = PlayerState.Loading
        _currentPosition.value = 0L
        _duration.value = 0L

        val mediaItem: MediaItem
        val mediaSource: ProgressiveMediaSource

        when (track.source) {
            TrackSource.LOCAL -> {
                val uri = Uri.parse(track.localUri ?: return)
                mediaItem = MediaItem.fromUri(uri)
                mediaSource = ProgressiveMediaSource.Factory(
                    DefaultDataSource.Factory(context)
                ).createMediaSource(mediaItem)
            }
            TrackSource.SMB -> {
                val dataSourceFactory = DataSource.Factory {
                    SmbDataSource(
                        inputStreamProvider = { smbFileBrowser.getInputStream(track.smbPath) },
                        fileSize = track.fileSize
                    )
                }
                mediaItem = MediaItem.Builder()
                    .setUri("smb://${track.smbPath}")
                    .setMediaId(track.smbPath)
                    .build()
                mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        if (startPositionMs > 0) {
            exoPlayer.seekTo(startPositionMs)
        }
    }

    fun play() {
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    fun stop() {
        stopPositionUpdates()
        exoPlayer.stop()
        _playerState.value = PlayerState.Idle
        _currentPosition.value = 0L
        currentTrack = null
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun getCurrentPositionMs(): Long = exoPlayer.currentPosition
    fun getDurationMs(): Long = exoPlayer.duration

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    fun setSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    fun getSpeed(): Float = (exoPlayer.playbackParameters?.speed ?: 1.0f)

    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private var abPointAL: Long = -1
    private var abPointBL: Long = -1

    fun setSleepTimer(mins: Int) {
        sleepTimerJob?.cancel()
        if (mins <= 0) return
        sleepTimerJob = scope.launch {
            kotlinx.coroutines.delay(mins * 60_000L)
            exoPlayer.pause()
        }
    }

    fun clearABLoop() { abPointAL = -1; abPointBL = -1 }

    fun setABLoopA(pos: Long) { abPointAL = pos }
    fun setABLoopB(pos: Long) { abPointBL = pos }
    fun getABLoop(): Pair<Long, Long> = Pair(abPointAL, abPointBL)

    fun release() {
        if (isReleased) return
        isReleased = true
        stopPositionUpdates()
        audioEffectManager.release()
        scope.cancel()
        exoPlayer.release()
    }
}
