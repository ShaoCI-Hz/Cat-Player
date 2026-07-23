package com.example.smbplayer.data.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.smbplayer.data.smb.SmbDataSource
import com.example.smbplayer.data.smb.SmbFileBrowser
import com.example.smbplayer.data.audio.AudioEffectManager
import com.example.smbplayer.data.audio.AudioDeviceManager
import com.example.smbplayer.data.audio.CrossfadeManager
import com.example.smbplayer.domain.ReplayGainProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smbFileBrowser: SmbFileBrowser,
    private val audioEffectManager: AudioEffectManager,
    val audioDeviceManager: AudioDeviceManager,
    val replayGainProcessor: ReplayGainProcessor,
    val crossfadeManager: CrossfadeManager
) {
    // Map from mediaId -> TrackInfo for SMB source resolution in playlist mode
    // Uses ConcurrentHashMap for thread safety (BUG-PR-01 fix)
    private val smbTrackMap = java.util.concurrent.ConcurrentHashMap<String, TrackInfo>()

    // DataSource.Factory that handles both local and SMB sources
    private val hybridDataSourceFactory = DataSource.Factory {
        object : DataSource {
            private var delegate: DataSource? = null
            override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {}
            override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
                val uri = dataSpec.uri
                delegate = if (uri.scheme == "smb") {
                    val smbPath = uri.host.orEmpty() + uri.path.orEmpty()
                    val track = smbTrackMap[smbPath]
                    if (track != null) {
                        SmbDataSource(
                            inputStreamProvider = { smbFileBrowser.getInputStream(track.smbPath) },
                            fileSize = track.fileSize,
                            smbUri = "smb://${track.smbPath}"
                        )
                    } else {
                        DefaultDataSource.Factory(context).createDataSource()
                    }
                } else {
                    DefaultDataSource.Factory(context).createDataSource()
                }
                return delegate!!.open(dataSpec)
            }
            override fun read(target: ByteArray, offset: Int, length: Int): Int =
                delegate?.read(target, offset, length) ?: -1
            override fun getUri(): Uri? = delegate?.uri
            override fun close() { delegate?.close() }
        }
    }

    private val mediaSourceFactory = ProgressiveMediaSource.Factory(hybridDataSourceFactory)

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

    private val _currentTrackIndex = MutableStateFlow(-1)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    // Emitted when ExoPlayer transitions to a new media item (for gapless playback)
    private val _mediaItemTransition = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val mediaItemTransition: SharedFlow<Int> = _mediaItemTransition.asSharedFlow()

    private var currentTrack: TrackInfo? = null
    private var playlistTracks: List<TrackInfo> = emptyList()
    private var positionJob: Job? = null
    private var isReleased = false

    init {
        audioEffectManager.attachExoPlayer(exoPlayer)
        crossfadeManager.attach(exoPlayer)
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

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    val newIndex = exoPlayer.currentMediaItemIndex
                    if (newIndex in playlistTracks.indices) {
                        currentTrack = playlistTracks[newIndex]
                        _currentTrackIndex.value = newIndex
                        _currentPosition.value = 0L
                        _mediaItemTransition.tryEmit(newIndex)
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
                val pos = exoPlayer.currentPosition
                _currentPosition.value = pos
                // N1: Check for crossfade trigger
                crossfadeManager.onPositionUpdate(pos, exoPlayer.duration)
                // BUG-PR-05 fix: Check AB loop
                if (abPointAL >= 0 && abPointBL > abPointAL && pos >= abPointBL) {
                    exoPlayer.seekTo(abPointAL)
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    fun getExoPlayer(): ExoPlayer = exoPlayer

    /**
     * Prepare a single track (legacy mode, still used for one-off playback).
     */
    fun prepare(track: TrackInfo, startPositionMs: Long = 0) {
        currentTrack = track
        playlistTracks = listOf(track)
        _currentTrackIndex.value = 0
        _playerState.value = PlayerState.Loading
        _currentPosition.value = 0L
        _duration.value = 0L

        // Clear and rebuild SMB track map
        smbTrackMap.clear()
        if (track.source == TrackSource.SMB) {
            smbTrackMap[track.smbPath] = track
        }

        val mediaSource = createMediaSource(track)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        if (startPositionMs > 0) {
            exoPlayer.seekTo(startPositionMs)
        }
    }

    /**
     * Prepare a playlist for gapless playback using ExoPlayer's native playlist mode.
     * ExoPlayer automatically preloads the next item for seamless transitions.
     */
    fun preparePlaylist(tracks: List<TrackInfo>, startIndex: Int = 0, startPositionMs: Long = 0) {
        if (tracks.isEmpty()) return

        playlistTracks = tracks
        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
        currentTrack = tracks[safeIndex]
        _currentTrackIndex.value = safeIndex
        _playerState.value = PlayerState.Loading
        _currentPosition.value = 0L
        _duration.value = 0L

        // Build SMB track map for DataSource resolution
        smbTrackMap.clear()
        tracks.filter { it.source == TrackSource.SMB }.forEach { track ->
            smbTrackMap[track.smbPath] = track
        }

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(
                    if (track.source == TrackSource.LOCAL) {
                        Uri.parse(track.localUri ?: "file:///dev/null")
                    } else {
                        Uri.parse("smb://${track.smbPath}")
                    }
                )
                .setMediaId(
                    if (track.source == TrackSource.LOCAL) track.localUri ?: ""
                    else track.smbPath
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }

        exoPlayer.setMediaItems(mediaItems, safeIndex, startPositionMs)
        exoPlayer.prepare()
    }

    /**
     * Get TrackInfo for a given index in the current playlist.
     */
    fun getTrackAt(index: Int): TrackInfo? =
        playlistTracks.getOrNull(index)

    fun getPlaylistSize(): Int = playlistTracks.size

    private fun createMediaSource(track: TrackInfo): MediaSource {
        return when (track.source) {
            TrackSource.LOCAL -> {
                val uri = Uri.parse(track.localUri ?: "file:///dev/null")
                val mediaItem = MediaItem.fromUri(uri)
                mediaSourceFactory.createMediaSource(mediaItem)
            }
            TrackSource.SMB -> {
                val mediaItem = MediaItem.Builder()
                    .setUri("smb://${track.smbPath}")
                    .setMediaId(track.smbPath)
                    .build()
                mediaSourceFactory.createMediaSource(mediaItem)
            }
            else -> {
                val mediaItem = MediaItem.fromUri("file:///dev/null")
                mediaSourceFactory.createMediaSource(mediaItem)
            }
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
        _duration.value = 0L
        currentTrack = null
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun seekToPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun seekToMediaItem(index: Int) {
        if (index in 0 until exoPlayer.mediaItemCount) {
            exoPlayer.seekToDefaultPosition(index)
        }
    }

    fun hasPlaylist(): Boolean = exoPlayer.mediaItemCount > 1

    fun getCurrentPositionMs(): Long = exoPlayer.currentPosition
    fun getDurationMs(): Long = exoPlayer.duration

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    fun setSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    fun getSpeed(): Float = (exoPlayer.playbackParameters?.speed ?: 1.0f)

    /**
     * Set playback speed and pitch independently (A6).
     * When pitch=1.0, changing speed won't affect pitch (time-stretching).
     * When speed=1.0, changing pitch won't affect speed (pitch-shifting).
     */
    fun setPitch(pitch: Float) {
        val currentSpeed = exoPlayer.playbackParameters?.speed ?: 1.0f
        exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(
            currentSpeed, pitch.coerceIn(0.5f, 2.0f)
        )
    }

    fun getPitch(): Float = (exoPlayer.playbackParameters?.pitch ?: 1.0f)

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

    /**
     * Apply ReplayGain to the current track based on its URI.
     * BUG-PR-04 fix: Uses baseVolume to avoid compounding across tracks.
     */
    private var baseVolume = 1f

    fun setVolumeWithGain(userVolume: Float) {
        baseVolume = userVolume.coerceIn(0f, 1f)
        exoPlayer.volume = baseVolume
    }

    suspend fun applyReplayGain(localUri: String?) {
        if (localUri.isNullOrEmpty()) return
        val gainInfo = replayGainProcessor.extractTrackGain(localUri)
        val linearGain = replayGainProcessor.getEffectiveGain(gainInfo)
        if (linearGain != null) {
            // Apply gain relative to base volume, not current volume
            exoPlayer.volume = (baseVolume * linearGain).coerceIn(0f, 1f)
        } else {
            // No gain info, restore base volume
            exoPlayer.volume = baseVolume
        }
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        stopPositionUpdates()
        audioEffectManager.release()
        audioDeviceManager.release()
        scope.cancel()
        exoPlayer.release()
    }
}
