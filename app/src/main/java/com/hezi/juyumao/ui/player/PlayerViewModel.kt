package com.hezi.juyumao.ui.player

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.repository.MetadataRepository
import com.hezi.juyumao.data.repository.SettingsRepository
import com.hezi.juyumao.player.MusicPlayerService
import com.hezi.juyumao.player.PlaybackStateHolder
import com.hezi.juyumao.player.audio.LyricsData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songDao: SongDao,
    private val metadataRepository: MetadataRepository,
    private val playbackStateHolder: PlaybackStateHolder,
    private val exoPlayer: ExoPlayer,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong

    private val _artworkUri = MutableStateFlow<String?>(null)
    val artworkUri: StateFlow<String?> = _artworkUri

    private val _lyrics = MutableStateFlow<LyricsData?>(null)
    val lyrics: StateFlow<LyricsData?> = _lyrics

    val isPlaying: StateFlow<Boolean> = playbackStateHolder.isPlaying
    val position: StateFlow<Long> = playbackStateHolder.position
    val duration: StateFlow<Long> = playbackStateHolder.duration
    val lyricsFontSize: StateFlow<Float> = settingsRepository.lyricsFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18f)
    val lyricsFontBold: StateFlow<Boolean> = settingsRepository.lyricsFontBold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        val songId = savedStateHandle.get<Long>("songId")
        if (songId != null && songId > 0) {
            loadSong(songId)
        }
        // 轮询进度
        viewModelScope.launch {
            while (true) {
                delay(500)
                playbackStateHolder.pollPosition()
            }
        }
    }

    private fun loadSong(songId: Long) {
        viewModelScope.launch {
            val song = songDao.getById(songId) ?: return@launch
            _currentSong.value = song
            playbackStateHolder.updateSong(song)

            // 更新播放记录
            songDao.update(song.copy(
                lastPlayedAt = System.currentTimeMillis(),
                playCount = song.playCount + 1,
            ))

            // 加载封面
            val artPath = metadataRepository.getCachedArtworkPath(song.id)
            _artworkUri.value = artPath
            playbackStateHolder.updateArtwork(artPath)

            if (artPath == null && song.source == "LOCAL") {
                try {
                    val newPath = metadataRepository.extractAndCacheArtwork(song)
                    _artworkUri.value = newPath
                    playbackStateHolder.updateArtwork(newPath)
                } catch (_: Exception) {}
            }

            // 加载歌词
            try {
                _lyrics.value = metadataRepository.getLyrics(song)
            } catch (_: Exception) {}

            // 设置 ExoPlayer 并播放
            exoPlayer.setMediaItem(MediaItem.fromUri(song.filePath))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            playbackStateHolder.updatePlaying(true)

            // 启动通知栏服务
            try {
                val intent = Intent(context, MusicPlayerService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {}
        }
    }

    fun seekTo(positionMs: Long) {
        playbackStateHolder.seekTo(positionMs)
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            playbackStateHolder.updatePlaying(false)
        } else {
            exoPlayer.play()
            playbackStateHolder.updatePlaying(true)
        }
    }
}
