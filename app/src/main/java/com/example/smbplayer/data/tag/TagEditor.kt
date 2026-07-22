package com.example.smbplayer.data.tag

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Edits audio file metadata tags using JAudioTagger.
 */
@Singleton
class TagEditor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class EditableTags(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val albumArtist: String = "",
        val genre: String = "",
        val year: String = "",
        val trackNumber: String = "",
        val comment: String = ""
    )

    /**
     * Read tags from a local audio file.
     */
    suspend fun readTags(localUri: String): EditableTags = withContext(Dispatchers.IO) {
        try {
            val path = getPathFromUri(localUri) ?: return@withContext EditableTags()
            val file = File(path)
            if (!file.exists()) return@withContext EditableTags()

            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: return@withContext EditableTags()

            EditableTags(
                title = tag.getFirst(FieldKey.TITLE),
                artist = tag.getFirst(FieldKey.ARTIST),
                album = tag.getFirst(FieldKey.ALBUM),
                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST),
                genre = tag.getFirst(FieldKey.GENRE),
                year = tag.getFirst(FieldKey.YEAR),
                trackNumber = tag.getFirst(FieldKey.TRACK),
                comment = tag.getFirst(FieldKey.COMMENT)
            )
        } catch (_: Exception) {
            EditableTags()
        }
    }

    /**
     * Write tags to a local audio file.
     * Returns true if successful.
     */
    suspend fun writeTags(localUri: String, tags: EditableTags): Boolean = withContext(Dispatchers.IO) {
        try {
            val path = getPathFromUri(localUri) ?: return@withContext false
            val file = File(path)
            if (!file.exists()) return@withContext false

            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, tags.title)
            tag.setField(FieldKey.ARTIST, tags.artist)
            tag.setField(FieldKey.ALBUM, tags.album)
            tag.setField(FieldKey.ALBUM_ARTIST, tags.albumArtist)
            tag.setField(FieldKey.GENRE, tags.genre)
            tag.setField(FieldKey.YEAR, tags.year)
            tag.setField(FieldKey.TRACK, tags.trackNumber)
            tag.setField(FieldKey.COMMENT, tags.comment)

            audioFile.commit()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getPathFromUri(uriStr: String): String? {
        val uri = Uri.parse(uriStr)
        if (uri.scheme == "file") return uri.path
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Audio.Media.DATA), null, null, null)
            return cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
        }
        return uri.path
    }
}
