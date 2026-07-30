package com.hezi.juyumao.data.repository

import android.content.Context
import com.hezi.juyumao.player.audio.LrcParser
import com.hezi.juyumao.player.audio.LyricsData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun findLyrics(songPath: String): LyricsData? = withContext(Dispatchers.IO) {
        val songFile = File(songPath)
        val baseName = songFile.nameWithoutExtension
        val parentDir = songFile.parentFile ?: return@withContext null

        // Try exact match: same name .lrc
        val lrcFile = File(parentDir, "$baseName.lrc")
        if (lrcFile.exists()) {
            return@withContext parseLrcFile(lrcFile)
        }

        // Try case-insensitive match
        parentDir.listFiles()?.find {
            it.extension.lowercase() == "lrc" &&
            it.nameWithoutExtension.equals(baseName, ignoreCase = true)
        }?.let {
            return@withContext parseLrcFile(it)
        }

        // Try embedded in same directory with similar name
        parentDir.listFiles()?.filter {
            it.extension.lowercase() == "lrc" &&
            it.nameWithoutExtension.contains(baseName.take(10), ignoreCase = true)
        }?.minByOrNull { it.name.length }?.let {
            return@withContext parseLrcFile(it)
        }

        null
    }

    private fun parseLrcFile(file: File): LyricsData? {
        return try {
            val content = file.readText(Charsets.UTF_8)
            val data = LrcParser.parse(content)
            if (data.lines.isNotEmpty()) data else null
        } catch (_: Exception) {
            null
        }
    }
}
