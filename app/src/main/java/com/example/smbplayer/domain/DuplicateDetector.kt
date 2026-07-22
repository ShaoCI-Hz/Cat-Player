package com.example.smbplayer.domain

import com.example.smbplayer.data.local.LocalTrack
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects duplicate tracks based on title + artist + duration similarity.
 */
@Singleton
class DuplicateDetector @Inject constructor() {

    data class DuplicateGroup(
        val tracks: List<LocalTrack>,
        val reason: String
    )

    /**
     * Find duplicate tracks in the library.
     * Matches by: exact title+artist, or similar duration (±2s) with same title.
     */
    fun findDuplicates(tracks: List<LocalTrack>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()

        // Group by normalized title + artist
        val titleArtistGroups = tracks.groupBy { track ->
            "${normalize(track.title)}|${normalize(track.artist)}"
        }.filter { it.value.size > 1 }

        titleArtistGroups.forEach { (_, group) ->
            groups.add(DuplicateGroup(group, "相同标题和歌手"))
        }

        // Group by similar duration + same title (different artists)
        val titleOnlyGroups = tracks.groupBy { normalize(it.title) }
            .filter { it.value.size > 1 }
            .filter { (title, _) ->
                // Only if not already caught by title+artist
                titleArtistGroups.none { it.key.startsWith(title) }
            }

        titleOnlyGroups.forEach { (_, group) ->
            // Check for similar durations (within 2 seconds)
            val durationClusters = clusterByDuration(group, 2000L)
            durationClusters.filter { it.size > 1 }.forEach { cluster ->
                groups.add(DuplicateGroup(cluster, "相同标题，相似时长"))
            }
        }

        return groups.sortedByDescending { it.tracks.size }
    }

    private fun normalize(text: String): String {
        return text.trim().lowercase()
            .replace(Regex("[\\s\\-_]+"), " ")
            .replace(Regex("[^a-z0-9\\s\\u4e00-\\u9fff]"), "")
    }

    private fun clusterByDuration(tracks: List<LocalTrack>, tolerance: Long): List<List<LocalTrack>> {
        val sorted = tracks.sortedBy { it.durationMs }
        val clusters = mutableListOf<MutableList<LocalTrack>>()
        var currentCluster = mutableListOf(sorted[0])

        for (i in 1 until sorted.size) {
            if (sorted[i].durationMs - currentCluster.first().durationMs <= tolerance) {
                currentCluster.add(sorted[i])
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(sorted[i])
            }
        }
        clusters.add(currentCluster)
        return clusters
    }
}
