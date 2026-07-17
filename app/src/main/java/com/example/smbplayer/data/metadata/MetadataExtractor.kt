package com.example.smbplayer.data.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.smbplayer.data.smb.SmbFileBrowser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val coverArt: Bitmap?,
    val mimeType: String?,
    val lyrics: String? = null
)

@Singleton
class MetadataExtractor @Inject constructor(
    private val smbFileBrowser: SmbFileBrowser,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val HEADER_SIZE = 256 * 1024L
        private const val BUFFER_SIZE = 65536
        private const val MAX_COVER_DIMEN = 512
    }

    suspend fun extract(smbPath: String, fileSize: Long): AudioMetadata =
        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("smb_meta_", null)
            try {
                val readSize = minOf(HEADER_SIZE, fileSize)
                if (readSize > 0) {
                    val input = smbFileBrowser.getInputStream(smbPath)
                    input.use { src ->
                        tempFile.outputStream().use { raw ->
                            BufferedOutputStream(raw, BUFFER_SIZE).use { dest ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var remaining = readSize
                                while (remaining > 0) {
                                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                    val bytesRead = src.read(buffer, 0, toRead)
                                    if (bytesRead == -1) break
                                    dest.write(buffer, 0, bytesRead)
                                    remaining -= bytesRead
                                }
                            }
                        }
                    }
                }

                val retriever = MediaMetadataRetriever()
                try {
                    if (tempFile.length() > 0) {
                        retriever.setDataSource(tempFile.absolutePath)
                        AudioMetadata(
                            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull(),
                            coverArt = retriever.embeddedPicture?.let { bytes ->
                                val opts = BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                                    inSampleSize = maxOf(outWidth / MAX_COVER_DIMEN, outHeight / MAX_COVER_DIMEN, 1)
                                    inJustDecodeBounds = false
                                }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                            },
                            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                        )
                    } else {
                        AudioMetadata(null, null, null, null, null, null)
                    }
                } finally {
                    retriever.release()
                }
            } finally {
                runCatching { tempFile.delete() }
            }
        }

    suspend fun extractFromUri(localUri: String): AudioMetadata = withContext(Dispatchers.IO) {
        if (localUri.isEmpty()) return@withContext AudioMetadata(null, null, null, null, null, null)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(localUri))
            AudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                coverArt = retriever.embeddedPicture?.let { bytes ->
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                        inSampleSize = maxOf(outWidth / MAX_COVER_DIMEN, outHeight / MAX_COVER_DIMEN, 1)
                        inJustDecodeBounds = false
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                },
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                lyrics = retriever.extractMetadata(8) // METADATA_KEY_LYRICS
            )
        } finally { retriever.release() }
    }

    /**
     * Comprehensive lyrics extraction: tries 3 methods in order
     * 1. jaudiotagger USLT (unsynchronized lyrics) / SYLT (synchronized lyrics) from ID3 tags
     * 2. MMR.extractMetadata(8) — METADATA_KEY_LYRICS
     * 3. .lrc file in the same directory as the audio file
     */
    suspend fun findLyrics(localUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(Uri.parse(localUri), arrayOf(android.provider.MediaStore.Audio.Media.DATA), null, null, null)
            val path = cursor?.use { if (it.moveToFirst()) it.getString(0) else null } ?: return@withContext null


            // Method 1: JAudioTagger — parse embedded lyrics from ID3 tags
            try {
                val audioFile = AudioFileIO.read(File(path))
                val tag = audioFile.tag
                val lyricsFromTag = tag?.getFirst(FieldKey.LYRICS)
                if (!lyricsFromTag.isNullOrBlank()) return@withContext lyricsFromTag
            } catch (_: Exception) {}

            // Method 2: MMR metadata key
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(localUri))
                val mmrLyrics = retriever.extractMetadata(8)
                retriever.release()
                if (!mmrLyrics.isNullOrBlank()) return@withContext mmrLyrics
            } catch (_: Exception) {}

            // Method 3: .lrc file in same directory
            val lrcPath = path.replaceAfterLast('.', "lrc")
            val lrcFile = File(lrcPath)
            if (lrcFile.exists()) return@withContext lrcFile.readText()

            null
        } catch (_: Exception) { null }
    }
}
