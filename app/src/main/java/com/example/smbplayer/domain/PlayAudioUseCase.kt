package com.example.smbplayer.domain

import com.example.smbplayer.data.metadata.AudioMetadata
import com.example.smbplayer.data.metadata.MetadataExtractor
import com.example.smbplayer.data.player.PlayerRepository
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import com.example.smbplayer.data.smb.SmbFileBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayAudioUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val metadataExtractor: MetadataExtractor,
    private val smbFileBrowser: SmbFileBrowser
) {
    private val _metadataUpdates = MutableStateFlow<AudioMetadata?>(null)
    val metadataUpdates: StateFlow<AudioMetadata?> = _metadataUpdates.asStateFlow()

    private val _lyricsUpdates = MutableStateFlow<List<com.example.smbplayer.data.lyrics.LyricLine>>(emptyList())
    val lyricsUpdates: StateFlow<List<com.example.smbplayer.data.lyrics.LyricLine>> = _lyricsUpdates.asStateFlow()

    suspend fun play(track: TrackInfo) {
        when (track.source) {
            TrackSource.LOCAL -> {
                playerRepository.prepare(track)
                playerRepository.play()
                try {
                    val metadata = metadataExtractor.extractFromUri(track.localUri ?: "")
                    _metadataUpdates.value = metadata
                    // Comprehensive lyrics search: embedded ID3 + MMR + .lrc
                    val lyrics = metadataExtractor.findLyrics(track.localUri ?: "")
                    if (lyrics != null) {
                        _lyricsUpdates.value = com.example.smbplayer.data.lyrics.LyricParser.parse(lyrics)
                    }
                } catch (_: Exception) {}
            }
            TrackSource.SMB -> {
                val actualSize = if (track.fileSize > 0) track.fileSize
                    else withContext(Dispatchers.IO) { smbFileBrowser.getFileSize(track.smbPath) }

                val fileName = track.smbPath.substringAfterLast('/')
                val basicTitle = fileName.substringBeforeLast('.')

                // 先播：用文件名作为临时标题，立刻开始播放
                val immediateTrack = track.copy(
                    title = basicTitle,
                    artist = "SMB 服务器",
                    album = "",
                    durationMs = 0,
                    fileSize = actualSize
                )
                playerRepository.prepare(immediateTrack)
                playerRepository.play()

                // 后取：后台下载元数据，播放器已先出声
                val metadata = metadataExtractor.extract(track.smbPath, actualSize)
                _metadataUpdates.value = metadata
            }
        }
    }

    fun pause() = playerRepository.pause()
    fun resume() = playerRepository.play()
    fun stop() = playerRepository.stop()
    fun seekTo(positionMs: Long) = playerRepository.seekTo(positionMs)
}
