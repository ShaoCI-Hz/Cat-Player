package com.example.smbplayer.ui.player

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smbplayer.data.player.PlayMode
import com.example.smbplayer.data.player.PlayerRepository
import com.example.smbplayer.data.player.PlayerState
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.settings.SettingsRepository
import com.example.smbplayer.domain.ManagePlaylistUseCase
import com.example.smbplayer.domain.PlayAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val playAudioUseCase: PlayAudioUseCase,
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerRepository.playerState
    val currentPosition: StateFlow<Long> = playerRepository.currentPosition
    val duration: StateFlow<Long> = playerRepository.duration

    private val _currentTrack = MutableStateFlow<TrackInfo?>(null)
    val currentTrack: StateFlow<TrackInfo?> = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<TrackInfo>>(emptyList())
    val playlist: StateFlow<List<TrackInfo>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.Sequential)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _coverArt = MutableStateFlow<ByteArray?>(null)
    val coverArt: StateFlow<ByteArray?> = _coverArt.asStateFlow()

    private val _playHistory = MutableStateFlow<List<TrackInfo>>(emptyList())
    val playHistory: StateFlow<List<TrackInfo>> = _playHistory.asStateFlow()

    private val _lyrics = MutableStateFlow<List<com.example.smbplayer.data.lyrics.LyricLine>>(emptyList())
    val lyrics: StateFlow<List<com.example.smbplayer.data.lyrics.LyricLine>> = _lyrics.asStateFlow()

    // Multi-playlist
    private val _savedPlaylists = MutableStateFlow<Map<String, List<TrackInfo>>>(emptyMap())
    val savedPlaylists: StateFlow<Map<String, List<TrackInfo>>> = _savedPlaylists.asStateFlow()

    fun saveCurrentAsPlaylist(name: String) {
        val current = _playlist.value
        if (current.isNotEmpty()) {
            _savedPlaylists.value = _savedPlaylists.value + (name to current)
        }
    }

    fun loadPlaylist(name: String) {
        _savedPlaylists.value[name]?.let { tracks ->
            _playlist.value = tracks
            if (tracks.isNotEmpty()) playTrack(tracks[0], tracks, 0)
        }
    }

    fun deletePlaylist(name: String) {
        _savedPlaylists.value = _savedPlaylists.value - name
    }

    fun getPlaylistNames(): List<String> = _savedPlaylists.value.keys.toList()

    init {
        viewModelScope.launch {
            _playMode.value = settingsRepository.playMode.first()
        }
        viewModelScope.launch {
            playAudioUseCase.metadataUpdates.collect { metadata ->
                if (metadata != null) {
                    _currentTrack.value = _currentTrack.value?.copy(
                        title = metadata.title ?: _currentTrack.value?.title ?: "未知曲目",
                        artist = metadata.artist ?: _currentTrack.value?.artist ?: "未知艺术家",
                        album = metadata.album ?: _currentTrack.value?.album ?: "未知专辑",
                        durationMs = metadata.durationMs ?: _currentTrack.value?.durationMs ?: 0,
                        coverArtBytes = metadata.coverArt?.let { bitmapToBytes(it) } ?: _currentTrack.value?.coverArtBytes,
                    )
                    metadata.coverArt?.let { bitmap ->
                        _coverArt.value = bitmapToBytes(bitmap)
                    }
                    // Parse embedded lyrics from metadata
                    if (!metadata.lyrics.isNullOrBlank()) {
                        _lyrics.value = parseLyrics(metadata.lyrics)
                    }
                }
            }
        }
        viewModelScope.launch {
            playAudioUseCase.lyricsUpdates.collect { lines ->
                _lyrics.value = lines
            }
        }
        // Restore last playback state
        viewModelScope.launch {
            val savedList = settingsRepository.savedPlaylist.first()
            val parsed = settingsRepository.parsePlaylist(savedList)
            if (parsed.isNotEmpty()) {
                _playlist.value = parsed
                _currentIndex.value = settingsRepository.savedIndex.first().coerceIn(-1, parsed.size - 1)
                _volume.value = settingsRepository.savedVolume.first()
                // Don't auto-play on restore, just restore state
            }
        }
        // Restore play history
        viewModelScope.launch {
            val saved = settingsRepository.savedPlayHistory.first()
            val parsed = settingsRepository.parsePlaylist(saved)
            _playHistory.value = parsed
        }
        // Periodically save position
        viewModelScope.launch {
            while (true) {
                delay(5000) // Save every 5 seconds
                try {
                    settingsRepository.savePlaybackState(
                        playlist = _playlist.value,
                        currentIndex = _currentIndex.value,
                        positionMs = playerRepository.getCurrentPositionMs(),
                        volume = _volume.value
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun playTrack(track: TrackInfo, playlist: List<TrackInfo>? = null, index: Int = -1) {
        if (playlist != null) _playlist.value = playlist
        if (index >= 0) _currentIndex.value = index
        else {
            _playlist.value = listOf(track)
            _currentIndex.value = 0
        }
        _currentTrack.value = track
        // Add to play history (max 100)
        val history = _playHistory.value.toMutableList()
        history.removeAll { it.smbPath == track.smbPath && it.localUri == track.localUri }
        history.add(0, track)
        if (history.size > 100) history.removeAt(history.lastIndex)
        _playHistory.value = history
        viewModelScope.launch { settingsRepository.savePlayHistory(_playHistory.value) }
        viewModelScope.launch {
            playAudioUseCase.play(track)
        }
    }

    fun togglePlay() {
        when (playerState.value) {
            is PlayerState.Playing -> playAudioUseCase.pause()
            is PlayerState.Paused -> playAudioUseCase.resume()
            else -> {}
        }
    }

    fun next() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val nextIdx = managePlaylistUseCase.getNextIndex(
            _currentIndex.value, list.size, _playMode.value
        )
        if (nextIdx >= 0) {
            playTrack(list[nextIdx], index = nextIdx)
        }
    }

    fun prev() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val prevIdx = managePlaylistUseCase.getPrevIndex(
            _currentIndex.value, list.size, _playMode.value
        )
        if (prevIdx >= 0) {
            playTrack(list[prevIdx], index = prevIdx)
        }
    }

    fun seekTo(positionMs: Long) {
        playAudioUseCase.seekTo(positionMs)
    }

    fun setVolume(vol: Float) {
        _volume.value = vol
        playerRepository.setVolume(vol)
    }

    fun setSpeed(speed: Float) {
        playerRepository.setSpeed(speed)
    }

    fun getSpeed(): Float = playerRepository.getSpeed()

    // AB Loop
    private var abPointA: Long = -1
    private var abPointB: Long = -1
    var abLoopActive: Boolean = false
        private set

    fun setABPointA() {
        abPointA = playerRepository.getCurrentPositionMs()
        abPointB = -1
        abLoopActive = false
    }

    fun setABPointB() {
        if (abPointA >= 0) {
            abPointB = playerRepository.getCurrentPositionMs()
            if (abPointB > abPointA) {
                abLoopActive = true
            }
        }
    }

    fun clearABLoop() {
        abPointA = -1
        abPointB = -1
        abLoopActive = false
    }

    fun checkABLoop() {
        if (!abLoopActive || abPointB < 0) return
        val pos = playerRepository.getCurrentPositionMs()
        if (pos >= abPointB) {
            playerRepository.seekTo(abPointA)
        }
    }

    fun getABPointA(): Long = abPointA
    fun getABPointB(): Long = abPointB

    // Sleep Timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()
    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerMinutes.value = 0
            _sleepTimerRemaining.value = 0
            return
        }
        _sleepTimerMinutes.value = minutes
        val deadline = System.currentTimeMillis() + minutes * 60_000L
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = deadline - System.currentTimeMillis()
                _sleepTimerRemaining.value = remaining
                if (remaining <= 0) {
                    playAudioUseCase.pause()
                    _sleepTimerMinutes.value = 0
                    _sleepTimerRemaining.value = 0
                    break
                }
                delay(1000)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerMinutes.value = 0
        _sleepTimerRemaining.value = 0
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
        viewModelScope.launch { settingsRepository.setPlayMode(mode) }
    }

    fun addToPlaylist(tracks: List<TrackInfo>) {
        val updated = _playlist.value.toMutableList()
        updated.addAll(tracks)
        _playlist.value = updated
    }

    fun removeFromPlaylist(index: Int) {
        val updated = _playlist.value.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _playlist.value = updated
            if (_currentIndex.value == index) stop()
            else if (_currentIndex.value > index) _currentIndex.value -= 1
        }
    }

    fun moveInPlaylist(from: Int, to: Int) {
        _playlist.value = managePlaylistUseCase.moveTrack(_playlist.value, from, to)
    }

    fun clearPlaylist() {
        stop()
        _playlist.value = emptyList()
        _currentIndex.value = -1
    }

    fun stop() {
        _currentTrack.value = null
        playAudioUseCase.stop()
    }
}

private fun bitmapToBytes(bmp: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return stream.toByteArray()
}

private fun parseLyrics(text: String): List<com.example.smbplayer.data.lyrics.LyricLine> {
    // Try LRC format first
    val lrcResult = com.example.smbplayer.data.lyrics.LyricParser.parse(text)
    if (lrcResult.isNotEmpty()) return lrcResult
    // Plain text: treat each line as a lyric without timestamp
    return text.lines().filter { it.isNotBlank() }.map { com.example.smbplayer.data.lyrics.LyricLine(0, it.trim()) }
}
