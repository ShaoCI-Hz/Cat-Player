package com.example.smbplayer.data.lyrics

data class LyricLine(val timestampMs: Long, val text: String)

object LyricParser {
    private val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    fun parse(lrcContent: String): List<LyricLine> {
        return lrcContent.lines().mapNotNull { line ->
            regex.find(line.trim())?.let { match ->
                val min = match.groupValues[1].toInt()
                val sec = match.groupValues[2].toInt()
                val ms = match.groupValues[3].padEnd(3, '0').toInt()
                val text = match.groupValues[4].trim()
                LyricLine((min.coerceIn(0, 99) * 60_000L) + sec * 1000L + ms, text.ifEmpty { null } ?: return@mapNotNull null)
            }
        }.sortedBy { it.timestampMs }
    }
}
