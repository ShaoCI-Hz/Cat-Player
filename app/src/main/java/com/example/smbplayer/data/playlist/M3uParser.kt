package com.example.smbplayer.data.playlist

import android.content.Context
import android.net.Uri
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses .m3u playlist files and creates TrackInfo objects.
 */
@Singleton
class M3uParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Parse an .m3u file from a content URI.
     * Returns a list of TrackInfo objects.
     */
    suspend fun parseFromUri(uri: Uri): List<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            val tracks = mutableListOf<TrackInfo>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream))

            var currentTitle = ""
            var currentDuration = 0L

            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("#EXTM3U") -> {} // Header, skip
                        trimmed.startsWith("#EXTINF:") -> {
                            // Parse: #EXTINF:duration,artist - title
                            val info = trimmed.removePrefix("#EXTINF:")
                            val parts = info.split(",", limit = 2)
                            currentDuration = parts.getOrNull(0)?.toLongOrNull()?.times(1000) ?: 0L
                            currentTitle = parts.getOrNull(1) ?: ""
                        }
                        trimmed.startsWith("#") -> {} // Other comments, skip
                        trimmed.isNotEmpty() -> {
                            // This is a file path or URL
                            val path = trimmed
                            val title = if (currentTitle.isNotEmpty()) {
                                currentTitle
                            } else {
                                path.substringAfterLast('/').substringBeforeLast('.')
                            }

                            val track = if (path.startsWith("smb://") || path.startsWith("\\\\")) {
                                TrackInfo(
                                    source = TrackSource.SMB,
                                    title = title,
                                    smbPath = path.removePrefix("smb://"),
                                    durationMs = currentDuration
                                )
                            } else {
                                TrackInfo(
                                    source = TrackSource.LOCAL,
                                    title = title,
                                    localUri = path,
                                    durationMs = currentDuration
                                )
                            }
                            tracks.add(track)
                            currentTitle = ""
                            currentDuration = 0L
                        }
                    }
                }
            }

            tracks
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Export a playlist to .m3u format string.
     */
    fun exportToM3u(tracks: List<TrackInfo>): String {
        return buildString {
            appendLine("#EXTM3U")
            tracks.forEach { track ->
                val duration = track.durationMs / 1000
                val path = if (track.source == TrackSource.SMB) {
                    "smb://${track.smbPath}"
                } else {
                    track.localUri ?: ""
                }
                appendLine("#EXTINF:$duration,${track.artist} - ${track.title}")
                appendLine(path)
            }
        }
    }
}
