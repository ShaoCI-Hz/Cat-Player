package com.hezi.juyumao.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.domain.model.RepeatMode
import com.hezi.juyumao.domain.model.Song
import com.hezi.juyumao.domain.model.SongSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val playbackStateHolder: PlaybackStateHolder,
) {
    private val queue = PlaybackQueue()

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
        playCurrent()
    }

    fun play() {
        exoPlayer.playWhenReady = true
        playbackStateHolder.updatePlaying(true)
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
        playCurrent()
    }

    fun previous() {
        val song = queue.previous(repeatMode) ?: return
        playCurrent()
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

    fun getQueue(): List<Song> = queue.songs.value

    // ── 内部 ──

    private fun playCurrent() {
        val song = queue.currentSong() ?: return
        val domain = song
        val artPath = playbackStateHolder.artworkUri.value

        val mediaItem = MediaItem.Builder()
            .setUri(domain.filePath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(domain.title)
                    .setArtist(domain.artist)
                    .setAlbumTitle(domain.album)
                    .also { builder ->
                        if (artPath != null) {
                            builder.setArtworkUri(Uri.parse("file://$artPath"))
                        }
                    }
                    .build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        playbackStateHolder.updatePlaying(true)
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
