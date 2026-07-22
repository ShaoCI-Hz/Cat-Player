package com.example.smbplayer.domain

import com.example.smbplayer.data.local.LocalTrack
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates smart playlists based on various criteria.
 */
@Singleton
class SmartPlaylistGenerator @Inject constructor() {

    /**
     * Generate "Recently Added" playlist - newest tracks by ID (higher ID = more recent).
     */
    fun recentlyAdded(tracks: List<LocalTrack>, limit: Int = 50): List<TrackInfo> {
        return tracks.sortedByDescending { it.id }
            .take(limit)
            .map { it.toTrackInfo() }
    }

    /**
     * Generate "Most Played" playlist from play stats.
     */
    fun mostPlayed(
        allTracks: List<LocalTrack>,
        playStats: Map<String, Int>,
        limit: Int = 50
    ): List<TrackInfo> {
        val trackMap = allTracks.associateBy { it.uri.toString() }
        return playStats.entries
            .sortedByDescending { it.value }
            .take(limit)
            .mapNotNull { (trackId, _) ->
                trackMap[trackId]?.toTrackInfo()
            }
    }

    /**
     * Generate "Random Mix" playlist.
     */
    fun randomMix(tracks: List<LocalTrack>, limit: Int = 100): List<TrackInfo> {
        return tracks.shuffled()
            .take(limit)
            .map { it.toTrackInfo() }
    }

    /**
     * Generate "Short Tracks" playlist (under 3 minutes).
     */
    fun shortTracks(tracks: List<LocalTrack>, limit: Int = 50): List<TrackInfo> {
        return tracks.filter { it.durationMs in 1..179_999 }
            .sortedBy { it.durationMs }
            .take(limit)
            .map { it.toTrackInfo() }
    }

    /**
     * Generate "Long Tracks" playlist (over 5 minutes).
     */
    fun longTracks(tracks: List<LocalTrack>, limit: Int = 50): List<TrackInfo> {
        return tracks.filter { it.durationMs > 300_000 }
            .sortedByDescending { it.durationMs }
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

/**
 * Smart playlist types available in the app.
 */
enum class SmartPlaylistType(val displayName: String, val icon: String) {
    RECENTLY_ADDED("最近添加", "new_releases"),
    MOST_PLAYED("最常播放", "trending_up"),
    RANDOM_MIX("随机歌单", "shuffle"),
    SHORT_TRACKS("短歌曲", "timer"),
    LONG_TRACKS("长歌曲", "all_inclusive")
}
