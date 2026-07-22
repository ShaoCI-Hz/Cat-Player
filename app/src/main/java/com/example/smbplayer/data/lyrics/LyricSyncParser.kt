package com.example.smbplayer.data.lyrics

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses synchronized lyrics from audio files.
 * Supports:
 * - SYLT (Synchronized Lyrics) ID3 tag for word-level sync
 * - LRC format for line-level sync
 * - USLT (Unsynchronized Lyrics) for plain text
 */
@Singleton
class LyricSyncParser @Inject constructor() {

    data class SyncedLyric(
        val timeMs: Long,
        val text: String
    )

    data class WordSyncedLyric(
        val lineTimeMs: Long,
        val words: List<WordTiming>
    )

    data class WordTiming(
        val timeMs: Long,
        val word: String
    )

    /**
     * Parse SYLT tag from an audio file for word-level synchronization.
     * Returns null if no SYLT tag is found.
     */
    fun parseSyltFromFile(filePath: String): List<WordSyncedLyric>? {
        try {
            val file = File(filePath)
            if (!file.exists()) return null

            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: return null

            // Try to get SYLT frame
            val syltField = tag.getFirst("SYLT")
            if (syltField.isNotBlank()) {
                return parseSyltString(syltField)
            }

            return null
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Parse SYLT data string.
     * Format: Each line is "timestamp|word" where timestamp is in milliseconds.
     */
    fun parseSyltString(data: String): List<WordSyncedLyric> {
        val lines = data.lines().filter { it.isNotBlank() }
        val result = mutableListOf<WordSyncedLyric>()

        var currentLineTime = 0L
        val currentWords = mutableListOf<WordTiming>()

        for (line in lines) {
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) {
                val timeMs = parts[0].toLongOrNull() ?: continue
                val word = parts[1]

                if (currentWords.isEmpty()) {
                    currentLineTime = timeMs
                }

                // Check if this is a new line (time gap > 500ms)
                if (currentWords.isNotEmpty() && timeMs - (currentWords.lastOrNull()?.timeMs ?: 0) > 500) {
                    result.add(WordSyncedLyric(currentLineTime, currentWords.toList()))
                    currentWords.clear()
                    currentLineTime = timeMs
                }

                currentWords.add(WordTiming(timeMs, word))
            }
        }

        // Add last line
        if (currentWords.isNotEmpty()) {
            result.add(WordSyncedLyric(currentLineTime, currentWords.toList()))
        }

        return result
    }

    /**
     * Parse enhanced LRC format with word-level timing.
     * Standard LRC: [mm:ss.xx]line text
     * Enhanced LRC: [mm:ss.xx]<mm:ss.xx>word1<mm:ss.xx>word2...
     */
    fun parseEnhancedLrc(lrcContent: String): List<WordSyncedLyric> {
        val result = mutableListOf<WordSyncedLyric>()
        val lineRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
        val wordRegex = Regex("<(\\d{2}):(\\d{2})\\.(\\d{2,3})>([^<]*)")

        for (line in lrcContent.lines()) {
            val lineMatch = lineRegex.find(line) ?: continue
            val lineTime = parseTime(lineMatch.groupValues[1], lineMatch.groupValues[2], lineMatch.groupValues[3])
            val content = lineMatch.groupValues[4]

            val wordMatches = wordRegex.findAll(content).toList()
            if (wordMatches.isNotEmpty()) {
                // Enhanced LRC with word timing
                val words = wordMatches.map { wm ->
                    val wordTime = parseTime(wm.groupValues[1], wm.groupValues[2], wm.groupValues[3])
                    WordTiming(wordTime, wm.groupValues[4])
                }
                result.add(WordSyncedLyric(lineTime, words))
            } else {
                // Standard LRC - treat entire line as one word
                val text = content.trim()
                if (text.isNotEmpty()) {
                    result.add(WordSyncedLyric(lineTime, listOf(WordTiming(lineTime, text))))
                }
            }
        }

        return result.sortedBy { it.lineTimeMs }
    }

    /**
     * Get the current word index for a given playback position.
     */
    fun getCurrentWordIndex(words: List<WordTiming>, positionMs: Long): Int {
        for (i in words.indices.reversed()) {
            if (positionMs >= words[i].timeMs) return i
        }
        return 0
    }

    /**
     * Get the current line index for a given playback position.
     */
    fun getCurrentLineIndex(lyrics: List<WordSyncedLyric>, positionMs: Long): Int {
        for (i in lyrics.indices.reversed()) {
            if (positionMs >= lyrics[i].lineTimeMs) return i
        }
        return 0
    }

    private fun parseTime(minutes: String, seconds: String, millis: String): Long {
        val m = minutes.toLongOrNull() ?: 0L
        val s = seconds.toLongOrNull() ?: 0L
        val ms = when (millis.length) {
            2 -> (millis.toLongOrNull() ?: 0L) * 10
            3 -> millis.toLongOrNull() ?: 0L
            else -> 0L
        }
        return m * 60_000 + s * 1000 + ms
    }
}
