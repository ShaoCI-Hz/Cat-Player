package com.example.smbplayer.smart

import com.example.smbplayer.data.local.LocalTrack
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import com.example.smbplayer.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local recommendation engine based on play history co-occurrence.
 * No cloud dependency - purely on-device analysis.
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Generate "For You" recommendations based on play history.
     */
    suspend fun generateRecommendations(
        allTracks: List<LocalTrack>,
        playStats: Map<String, Int>
    ): List<TrackInfo> {
        if (allTracks.isEmpty() || playStats.isEmpty()) return emptyList()

        // Get top played tracks
        val topPlayed = playStats.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        // Find tracks by same artists as top played
        val topArtists = topPlayed.mapNotNull { trackId ->
            allTracks.find { it.uri.toString() == trackId }?.artist
        }.distinct().take(5)

        // Recommend tracks from same artists that aren't in top played
        val recommendations = allTracks
            .filter { track ->
                track.artist in topArtists &&
                track.uri.toString() !in topPlayed
            }
            .shuffled()
            .take(20)
            .map { it.toTrackInfo() }

        return recommendations
    }

    /**
     * Generate "Today's Mix" - random selection weighted by play count.
     */
    suspend fun generateTodayMix(
        allTracks: List<LocalTrack>,
        playStats: Map<String, Int>
    ): List<TrackInfo> {
        if (allTracks.isEmpty()) return emptyList()

        // Weight tracks by play count (more played = higher chance)
        val weighted = allTracks.map { track ->
            val weight = (playStats[track.uri.toString()] ?: 0) + 1
            track to weight
        }

        // Weighted random selection
        val selected = mutableListOf<LocalTrack>()
        val remaining = weighted.toMutableList()

        repeat(minOf(15, allTracks.size)) {
            if (remaining.isEmpty()) return@repeat
            val totalWeight = remaining.sumOf { it.second }
            var random = (0 until totalWeight).random()
            for ((track, weight) in remaining) {
                random -= weight
                if (random < 0) {
                    selected.add(track)
                    remaining.remove(track to weight)
                    break
                }
            }
        }

        return selected.map { it.toTrackInfo() }
    }

    /**
     * Generate "Recently Added" recommendations.
     */
    fun generateRecentlyAdded(allTracks: List<LocalTrack>, limit: Int = 20): List<TrackInfo> {
        return allTracks
            .sortedByDescending { it.id }
            .take(limit)
            .map { it.toTrackInfo() }
    }

    private fun LocalTrack.toTrackInfo() = TrackInfo(
        source = TrackSource.LOCAL,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        localUri = uri.toString()
    )
}
