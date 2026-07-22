package com.example.smbplayer.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import com.example.smbplayer.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class LocalTrack(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val size: Long,
    val mimeType: String,
    val albumId: Long = 0
) {
    fun albumArtUri(): Uri? = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
}

data class AlbumEntry(val name: String, val artist: String, val tracks: List<LocalTrack>)

data class ArtistEntry(val name: String, val tracks: List<LocalTrack>)

@Singleton
class LocalMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        val DEFAULT_EXCLUDED_FOLDERS = setOf(
            "Ringtones", "Notifications", "Alarms", "Podcasts", "Audiobooks", "Recordings"
        )
    }

    suspend fun loadAllTracks(): List<LocalTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalTrack>()
        val resolver: ContentResolver = context.contentResolver

        // Read configurable settings
        val customExcluded = settingsRepository.excludedFolders.first()
        val minDurationSec = settingsRepository.minDurationSeconds.first()
        val allExcluded = DEFAULT_EXCLUDED_FOLDERS + customExcluded
        val minDurationMs = (minDurationSec * 1000L).coerceAtLeast(1000L)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataColumn) ?: ""
                // Skip excluded folders
                if (allExcluded.any { "/$it/" in filePath || filePath.contains("/$it/", ignoreCase = true) }) continue

                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                tracks.add(
                    LocalTrack(
                        id = id,
                        uri = uri,
                        title = cursor.getString(titleColumn) ?: "未知曲目",
                        artist = cursor.getString(artistColumn) ?: "未知艺术家",
                        album = cursor.getString(albumColumn) ?: "",
                        durationMs = cursor.getLong(durationColumn),
                        size = cursor.getLong(sizeColumn),
                        mimeType = cursor.getString(mimeColumn) ?: "audio/*",
                        albumId = cursor.getLong(albumIdColumn)
                    )
                )
            }
        }

        tracks
    }

    fun toTrackInfo(track: LocalTrack): TrackInfo = TrackInfo(
        source = TrackSource.LOCAL,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationMs = track.durationMs,
        localUri = track.uri.toString()
    )
}
