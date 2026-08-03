package com.hezi.juyumao.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.hezi.juyumao.data.local.crypto.decryptPassword
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import com.hezi.juyumao.domain.model.RepeatMode
import com.hezi.juyumao.domain.model.Song
import com.hezi.juyumao.domain.model.SongSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val playbackStateHolder: PlaybackStateHolder,
    private val smbConnectionPool: SmbConnectionPool,
    private val serverDao: ServerDao,
) {
    private val queue = PlaybackQueue()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
        scope.launch { playCurrent() }
    }

    fun play() {
        exoPlayer.playWhenReady = true
        // isPlaying 由 ExoPlayer 的 onIsPlayingChanged 回调同步，不在此处虚报
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        playbackStateHolder.updatePlaying(false)
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) pause() else play()
    }

    fun next() {
        val song = queue.next(repeatMode, shuffleEnabled) ?: return
        scope.launch { playCurrent() }
    }

    fun previous() {
        val song = queue.previous(repeatMode) ?: return
        scope.launch { playCurrent() }
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
        scope.launch { playCurrent() }
    }

    fun clearQueue() {
        queue.clear()
        exoPlayer.stop()
        playbackStateHolder.updateSong(null)
    }

    // ── 内部 ──

    private suspend fun playCurrent() {
        val song = queue.currentSong() ?: return
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
)
