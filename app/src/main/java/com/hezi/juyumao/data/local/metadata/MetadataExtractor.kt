package com.hezi.juyumao.data.local.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import com.hezi.juyumao.data.remote.smb.AudioFileFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地音频文件元数据提取器
 * 双层方案：MediaMetadataRetriever 做基础 + jaudiotagger 做增强
 */
@Singleton
class MetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * 提取本地文件的完整元数据
     */
    suspend fun extract(filePath: String): AudioMetadata = withContext(Dispatchers.IO) {
        // 先用 jaudiotagger 提取完整元数据（含歌词）
        try {
            extractWithJAudioTagger(filePath)
        } catch (_: Exception) {
            // 回退到 MediaMetadataRetriever
            extractWithRetriever(filePath)
        }
    }

    private fun extractWithJAudioTagger(filePath: String): AudioMetadata {
        val audioFile = AudioFileIO.read(File(filePath))
        val tag = audioFile.tag
        val header = audioFile.audioHeader

        if (tag == null) {
            return extractWithRetriever(filePath)
        }

        // 读取基础标签
        val title = tag.getFirst(FieldKey.TITLE).ifEmpty { null }
        val artist = tag.getFirst(FieldKey.ARTIST).ifEmpty { null }
        val album = tag.getFirst(FieldKey.ALBUM).ifEmpty { null }
        val albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST).ifEmpty { null }
        val trackNumber = tag.getFirst(FieldKey.TRACK).split("/").firstOrNull()?.toIntOrNull()
        val totalTracks = tag.getFirst(FieldKey.TRACK).split("/").getOrNull(1)?.toIntOrNull()
        val discNumber = tag.getFirst(FieldKey.DISC_NO).toIntOrNull()
        val year = tag.getFirst(FieldKey.YEAR).toIntOrNull()
        val genre = tag.getFirst(FieldKey.GENRE).ifEmpty { null }
        val composer = tag.getFirst(FieldKey.COMPOSER).ifEmpty { null }

        // 读取内嵌歌词
        val lyrics = readEmbeddedLyrics(tag)

        // 读取封面
        val artwork = tag.firstArtwork

        // 读取音频参数
        val sampleRate = (header?.sampleRateAsNumber ?: 0).toInt()
        val bitsPerSample = (header?.bitsPerSample ?: 0).toInt()
        val channels = 0
        val bitrate = (header?.bitRateAsNumber ?: 0).toInt()
        val durationMs: Long = (header?.trackLength ?: 0).toLong() * 1000L

        return AudioMetadata(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            trackNumber = trackNumber,
            totalTracks = totalTracks,
            discNumber = discNumber,
            year = year,
            genre = genre,
            composer = composer,
            duration = durationMs,
            bitrate = bitrate,
            sampleRate = sampleRate,
            bitsPerSample = bitsPerSample,
            channels = channels,
            mimeType = AudioFileFilter.getMimeType(filePath),
            fileSize = File(filePath).length(),
            artworkData = artwork?.binaryData,
            artworkMimeType = artwork?.mimeType,
            embeddedLyrics = lyrics,
        )
    }

    private fun extractWithRetriever(filePath: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            return AudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.split("/")?.firstOrNull()?.toIntOrNull(),
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull(),
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.div(1000) ?: 0,
                artworkData = retriever.embeddedPicture,
                artworkMimeType = getArtworkMime(retriever.embeddedPicture),
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "",
                fileSize = File(filePath).length(),
            )
        } finally {
            retriever.release()
        }
    }

    private fun readEmbeddedLyrics(tag: org.jaudiotagger.tag.Tag): String? {
        // 1. 通用 FieldKey.LYRICS（覆盖 Vorbis LYRICS / MP4 ©lyr / APE Lyrics）
        try {
            tag.getFirst(FieldKey.LYRICS).takeIf { it.isNotBlank() }?.let { return it }
        } catch (_: Exception) {}

        // 2. ID3v2 USLT 帧（MP3 常见）
        if (tag is org.jaudiotagger.tag.id3.AbstractID3v2Tag) {
            try {
                val frame = tag.getFrame("USLT")
                if (frame is org.jaudiotagger.tag.id3.AbstractTagFrame) {
                    val body = frame.body
                    if (body is org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT) {
                        val lyric: String? = body.toString().takeIf { it.isNotBlank() && it != "Unsyncronised lyrics frame" }
                        if (lyric != null) return lyric
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun getArtworkMime(data: ByteArray?): String? {
        if (data == null || data.size < 4) return null
        return when {
            data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() -> "image/jpeg"
            data[0] == 0x89.toByte() && data[1] == 0x50.toByte() -> "image/png"
            else -> "image/jpeg"
        }
    }
}
