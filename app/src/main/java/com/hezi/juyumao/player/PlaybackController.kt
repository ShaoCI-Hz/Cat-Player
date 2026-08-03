package com.hezi.juyumao.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.hezi.juyumao.data.local.crypto.decryptPassword
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import com.hezi.juyumao.data.repository.SettingsRepository
import com.hezi.juyumao.domain.model.RepeatMode
import com.hezi.juyumao.domain.model.Song
import com.hezi.juyumao.domain.model.SongSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val playbackStateHolder: PlaybackStateHolder,
    private val smbConnectionPool: SmbConnectionPool,
    private val serverDao: ServerDao,
    private val songDao: SongDao,
    private val dynamicLoadControl: DynamicLoadControl,
    private val settingsRepository: SettingsRepository,
) {
    private val queue = PlaybackQueue()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** 本地文件 MediaSource 工厂（构建整队列时用） */
    private val localSourceFactory = DefaultMediaSourceFactory(context)

    /** 最近一次播放错误的当前歌曲 id，用于防死循环（连续同一首歌失败则不再自动跳过） */
    private var lastErrorSongId: Long? = null

    /** 淡入淡出任务（防止并发交叉） */
    private var fadeJob: Job? = null

    /** 无缝播放开关（缓存设置值） */
    @Volatile private var gaplessEnabled: Boolean = false

    /** 交叉淡化时长 ms（0 = 关闭） */
    @Volatile private var crossfadeMs: Int = 0

    init {
        // 解码失败兜底：提示 + 自动跳过下一首（T9.5）
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val song = queue.currentSong()
                playbackStateHolder.setErrorMessage("无法播放该格式，已跳过")
                val songId = song?.id
                if (songId != null && songId != lastErrorSongId) {
                    lastErrorSongId = songId
                    scope.launch { next() }
                } else {
                    // 连续同一首失败：停止，避免死循环
                    lastErrorSongId = null
                    playbackStateHolder.updatePlaying(false)
                }
            }
        })

        // 读取无缝/淡化设置 + 倍速
        scope.launch {
            gaplessEnabled = try { settingsRepository.gaplessPlayback.first() } catch (_: Exception) { false }
            crossfadeMs = try { settingsRepository.crossfadeDuration.first() } catch (_: Exception) { 0 }
            val speed = try { settingsRepository.playbackSpeed.first() } catch (_: Exception) { 1.0f }
            if (speed > 0f && speed != 1.0f) exoPlayer.setPlaybackSpeed(speed)
        }

        // 播放统计埋点：曲目切换时递增 playCount（T10.9）
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val song = queue.currentSong() ?: return
                scope.launch {
                    try { songDao.incrementPlayCount(song.id) } catch (_: Exception) {}
                }
            }
        })
    }

    /** 当前播放模式：0=OFF, 1=ALL, 2=ONE */
    private var repeatModeIndex: Int = 0

    /** 是否随机 */
    private var shuffleEnabled: Boolean = false

    private val repeatMode: RepeatMode
        get() = when (repeatModeIndex) {
            1 -> RepeatMode.ALL
            2 -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }

    // ── 公开接口 ──

    fun loadPlaylist(songs: List<SongEntity>, startIndex: Int = 0) {
        val domainSongs = songs.map { it.toDomain() }
        queue.setQueue(domainSongs, startIndex)
        scope.launch {
            // 无缝播放开启时整队列加载，由 ExoPlayer 衔接曲目
            if (gaplessEnabled && songs.size > 1) {
                val sources = buildMediaSources(songs)
                exoPlayer.setMediaSources(sources, startIndex.coerceIn(0, sources.size - 1), 0L)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                playCurrent()
            }
        }
    }

    fun play() {
        exoPlayer.playWhenReady = true
        // 淡入（交叉淡化开启时）
        if (crossfadeMs > 0) {
            fadeJob?.cancel()
            fadeJob = scope.launch { fadeVolume(from = 0f, to = 1f) }
        }
        // isPlaying 由 ExoPlayer 的 onIsPlayingChanged 回调同步，不在此处虚报
    }

    fun pause() {
        // 淡出后暂停（交叉淡化开启时）
        if (crossfadeMs > 0 && exoPlayer.isPlaying) {
            fadeJob?.cancel()
            fadeJob = scope.launch {
                fadeVolume(from = 1f, to = 0f)
                exoPlayer.playWhenReady = false
            }
        } else {
            exoPlayer.playWhenReady = false
        }
        playbackStateHolder.updatePlaying(false)
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) pause() else play()
    }

    fun next() {
        val song = queue.next(repeatMode, shuffleEnabled) ?: return
        scope.launch {
            if (gaplessEnabled && exoPlayer.mediaItemCount > 1) {
                // 无缝模式：ExoPlayer 已加载整队列，直接跳转
                exoPlayer.seekToDefaultPosition(queue.currentIndex.value)
                exoPlayer.play()
            } else {
                playCurrent()
            }
        }
    }

    fun previous() {
        val song = queue.previous(repeatMode) ?: return
        scope.launch {
            if (gaplessEnabled && exoPlayer.mediaItemCount > 1) {
                exoPlayer.seekToDefaultPosition(queue.currentIndex.value)
                exoPlayer.play()
            } else {
                playCurrent()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        playbackStateHolder.seekTo(positionMs)
    }

    fun setShuffle(enabled: Boolean) {
        shuffleEnabled = enabled
    }

    fun setRepeat(modeIndex: Int) {
        repeatModeIndex = modeIndex
    }

    fun currentSong(): Song? = queue.currentSong()
        ?: playbackStateHolder.currentSong.value?.toDomain()

    fun getQueue(): List<Song> = queue.songs.value

    fun getQueueIndex(): Int = queue.currentIndex.value

    fun playAt(index: Int) {
        queue.playAt(index)
        scope.launch {
            if (gaplessEnabled && exoPlayer.mediaItemCount > 1) {
                exoPlayer.seekToDefaultPosition(queue.currentIndex.value)
                exoPlayer.play()
            } else {
                playCurrent()
            }
        }
    }

    fun clearQueue() {
        queue.clear()
        exoPlayer.stop()
        playbackStateHolder.updateSong(null)
    }

    /** 设置倍速并持久化（倍速下音高不变，Media3 内置支持） */
    fun setPlaybackSpeed(speed: Float) {
        val safe = speed.coerceIn(0.25f, 2.0f)
        exoPlayer.setPlaybackSpeed(safe)
        scope.launch {
            try { settingsRepository.setPlaybackSpeed(safe) } catch (_: Exception) {}
        }
    }

    // ── 内部 ──

    /** 为整队列构建 MediaSource（SMB 歌曲逐首连接，失败降级为单曲模式） */
    private suspend fun buildMediaSources(songs: List<SongEntity>): List<MediaSource> {
        val sources = mutableListOf<MediaSource>()
        for (song in songs) {
            val domain = song.toDomain()
            val artPath = playbackStateHolder.artworkUri.value
            val metadata = MediaMetadata.Builder()
                .setTitle(domain.title)
                .setArtist(domain.artist)
                .setAlbumTitle(domain.album)
                .also { builder ->
                    if (artPath != null) builder.setArtworkUri(Uri.parse("file://$artPath"))
                }
                .build()

            if (domain.source == SongSource.SMB && song.smbServerId != null && song.smbSharePath != null) {
                try {
                    val server = serverDao.getServerById(song.smbServerId)?.decryptPassword()
                    if (server != null) {
                        val smbClient = smbConnectionPool.getConnection(
                            serverId = server.id,
                            host = server.ip,
                            port = server.port,
                            username = server.username,
                            password = server.password,
                            shareName = server.effectiveShareName,
                        )
                        sources.add(createSmbMediaSource(smbClient, song.smbSharePath, song.mimeType, metadata))
                        continue
                    }
                } catch (_: Exception) {}
                // SMB 连接失败：降级为本地 MediaItem（播放时由错误兜底跳过）
                sources.add(
                    localSourceFactory.createMediaSource(
                        MediaItem.Builder()
                            .setUri(song.smbSharePath)
                            .setMediaMetadata(metadata)
                            .build()
                    )
                )
            } else {
                sources.add(
                    localSourceFactory.createMediaSource(
                        MediaItem.Builder()
                            .setUri(domain.filePath)
                            .setMediaMetadata(metadata)
                            .build()
                    )
                )
            }
        }
        return sources
    }

    private suspend fun playCurrent() {
        val song = queue.currentSong() ?: return
        // 动态缓冲：HiRes 歌曲用更大预缓冲（SMB 大文件防卡顿）
        val bufferKb = try { settingsRepository.audioBufferSize.first() } catch (_: Exception) { 256 }
        dynamicLoadControl.updateSettings(bufferKb, song.isHiRes)
        val artPath = playbackStateHolder.artworkUri.value

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .also { builder ->
                if (artPath != null) {
                    builder.setArtworkUri(Uri.parse("file://$artPath"))
                }
            }
            .build()

        if (song.source == SongSource.SMB && song.smbServerId != null && song.smbSharePath != null) {
            // SMB 歌曲：使用 SmbMediaSource（metadata 在 MediaSource 内，避免 setMediaItem 被覆盖）
            val server = serverDao.getServerById(song.smbServerId)?.decryptPassword()
            if (server != null) {
                try {
                    val smbClient = smbConnectionPool.getConnection(
                        serverId = server.id,
                        host = server.ip,
                        port = server.port,
                        username = server.username,
                        password = server.password,
                        shareName = server.effectiveShareName,
                    )
                    val mediaSource = createSmbMediaSource(smbClient, song.smbSharePath, song.mimeType, metadata)
                    exoPlayer.setMediaSource(mediaSource)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    return
                } catch (_: Exception) {
                    // SMB 连接失败，跳过
                }
            }
        }

        // 本地歌曲或 SMB 失败回退
        val mediaItem = MediaItem.Builder()
            .setUri(song.filePath)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    /** 音量渐变（淡入淡出/交叉淡化，Media3 1.5.1 无 AudioFade API，手动实现） */
    private suspend fun fadeVolume(from: Float, to: Float) {
        if (crossfadeMs <= 0) return
        val steps = 20
        val stepMs = crossfadeMs / steps
        for (i in 1..steps) {
            val progress = i.toFloat() / steps
            val volume = from + (to - from) * progress
            try { exoPlayer.setVolume(volume.coerceIn(0f, 1f)) } catch (_: Exception) {}
            delay(stepMs.toLong())
        }
    }
}

/** SongEntity -> Song 领域模型转换 */
fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumArtUri = albumArtUri,
    duration = duration,
    filePath = filePath,
    fileSize = fileSize,
    mimeType = mimeType,
    isHiRes = isHiRes,
    source = if (source == "SMB") SongSource.SMB else SongSource.LOCAL,
    smbServerId = smbServerId,
    smbSharePath = smbSharePath,
    sampleRate = sampleRate,
    bitsPerSample = bitsPerSample,
    bitrate = bitrate,
)
