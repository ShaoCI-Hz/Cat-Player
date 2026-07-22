package com.example.smbplayer.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Last.fm Scrobbling support.
 * Tracks played songs and submits scrobbles to Last.fm API.
 * Note: Full API integration requires an API key and user authentication.
 * This is a placeholder implementation that stores scrobble data locally.
 */
@Singleton
class Scrobbler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class ScrobbleTrack(
        val artist: String,
        val title: String,
        val album: String = "",
        val timestamp: Long = System.currentTimeMillis() / 1000
    )

    // Pending scrobbles queue (would be persisted in production)
    private val pendingScrobbles = mutableListOf<ScrobbleTrack>()

    // Minimum play duration before scrobbling (in seconds)
    private val minPlayDuration = 30

    // Minimum percentage of track to play before scrobbling
    private val minPlayPercentage = 0.5f

    /**
     * Check if a track should be scrobbled based on play duration.
     */
    fun shouldScrobble(durationPlayedMs: Long, totalDurationMs: Long): Boolean {
        if (totalDurationMs <= 0) return false
        val durationPlayedSec = durationPlayedMs / 1000
        val totalDurationSec = totalDurationMs / 1000
        return durationPlayedSec >= minPlayDuration ||
               durationPlayedSec >= (totalDurationSec * minPlayPercentage).toLong()
    }

    /**
     * Queue a track for scrobbling.
     */
    fun queueScrobble(track: ScrobbleTrack) {
        pendingScrobbles.add(track)
    }

    /**
     * Get pending scrobble count.
     */
    fun getPendingCount(): Int = pendingScrobbles.size

    /**
     * Get all pending scrobbles.
     */
    fun getPendingScrobbles(): List<ScrobbleTrack> = pendingScrobbles.toList()

    /**
     * Clear pending scrobbles after successful submission.
     */
    fun clearPending() {
        pendingScrobbles.clear()
    }

    /**
     * Format scrobble data for Last.fm API submission.
     * In production, this would POST to ws.audioscrobbler.com
     */
    fun formatScrobblesForApi(tracks: List<ScrobbleTrack>): String {
        return tracks.joinToString("\n") { track ->
            "${track.artist} - ${track.title} (${track.album})"
        }
    }
}
