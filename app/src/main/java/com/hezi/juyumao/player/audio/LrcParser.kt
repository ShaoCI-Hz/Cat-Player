package com.hezi.juyumao.player.audio

import androidx.compose.runtime.Immutable

@Immutable
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord>? = null,
)

@Immutable
data class LyricWord(
    val timeMs: Long,
    val durationMs: Long,
    val text: String,
)

@Immutable
data class LyricsData(
    val lines: List<LyricLine>,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
)

object LrcParser {

    private val timePattern = Regex("""\[(\d{2}):(\d{2})[.:](\d{2,3})]""")
    private val metaPattern = Regex("""\[(\w+):(.+?)]""")

    fun parse(lrcContent: String): LyricsData {
        val lines = mutableListOf<LyricLine>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null

        for (line in lrcContent.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check for metadata
            val metaMatch = metaPattern.matchEntire(trimmed)
            if (metaMatch != null) {
                val key = metaMatch.groupValues[1]
                val value = metaMatch.groupValues[2]
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                }
                continue
            }

            // Parse timed lyrics: [mm:ss.xx]text
            val timeMatches = timePattern.findAll(trimmed).toList()
            if (timeMatches.isEmpty()) continue

            val text = trimmed.substringAfterLast("]").trim()
            if (text.isEmpty()) continue

            for (timeMatch in timeMatches) {
                val minutes = timeMatch.groupValues[1].toLong()
                val seconds = timeMatch.groupValues[2].toLong()
                val millis = timeMatch.groupValues[3].let {
                    if (it.length == 2) it.toLong() * 10 else it.toLong()
                }
                val totalMs = minutes * 60_000 + seconds * 1000 + millis

                lines.add(LyricLine(timeMs = totalMs, text = text))
            }
        }

        return LyricsData(
            lines = lines.sortedBy { it.timeMs },
            title = title,
            artist = artist,
            album = album,
        )
    }

    fun findCurrentLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
