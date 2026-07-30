package com.hezi.juyumao.data.local.lyrics

import android.content.Context
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.local.metadata.MetadataExtractor
import com.hezi.juyumao.data.remote.smb.SmbClientWrapper
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import com.hezi.juyumao.player.audio.LrcParser
import com.hezi.juyumao.player.audio.LyricsData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一歌词管理：内嵌 + 外挂 .lrc，本地 + SMB
 */
@Singleton
class LyricsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataExtractor: MetadataExtractor,
    private val smbConnectionPool: SmbConnectionPool,
) {
    /**
     * 获取歌词（自动判断来源和类型）
     */
    suspend fun getLyrics(song: SongEntity): LyricsData? = withContext(Dispatchers.IO) {
        when (song.source) {
            "LOCAL" -> getLyricsLocal(song.filePath)
            "SMB" -> getLyricsSmb(song)
            else -> null
        }
    }

    /** 本地歌词查找 */
    private suspend fun getLyricsLocal(filePath: String): LyricsData? {
        // 1. 外挂 .lrc
        findExternalLrc(filePath)?.let { return it }
        // 2. 内嵌歌词
        findEmbeddedLyrics(filePath)?.let { return it }
        // 3. 同目录 .txt
        findExternalTxt(filePath)?.let { return it }
        return null
    }

    /** SMB 歌词查找 */
    private suspend fun getLyricsSmb(song: SongEntity): LyricsData? {
        // 1. SMB 同目录 .lrc
        findSmbLrc(song)?.let { return it }
        // 2. SMB 内嵌歌词
        findSmbEmbeddedLyrics(song)?.let { return it }
        return null
    }

    /** 查找外挂 .lrc 文件 */
    private fun findExternalLrc(filePath: String): LyricsData? {
        val songFile = File(filePath)
        val baseName = songFile.nameWithoutExtension
        val parentDir = songFile.parentFile ?: return null

        val lrcFile = parentDir.listFiles()?.find {
            it.extension.lowercase() == "lrc" &&
            it.nameWithoutExtension.equals(baseName, ignoreCase = true)
        } ?: return null

        return parseLyricsFile(lrcFile)
    }

    /** 查找外挂 .txt 文件 */
    private fun findExternalTxt(filePath: String): LyricsData? {
        val songFile = File(filePath)
        val baseName = songFile.nameWithoutExtension
        val parentDir = songFile.parentFile ?: return null

        val txtFile = parentDir.listFiles()?.find {
            it.extension.lowercase() == "txt" &&
            it.nameWithoutExtension.equals(baseName, ignoreCase = true)
        } ?: return null

        return parsePlainText(txtFile.readText(Charsets.UTF_8))
    }

    /** 读取内嵌歌词 */
    private suspend fun findEmbeddedLyrics(filePath: String): LyricsData? {
        val meta = metadataExtractor.extract(filePath)
        val lyricsText = meta.embeddedLyrics ?: return null
        return parseLyricsContent(lyricsText)
    }

    /** 从 SMB 查找外挂 .lrc */
    private suspend fun findSmbLrc(song: SongEntity): LyricsData? {
        val serverId = song.smbServerId ?: return null
        val sharePath = song.smbSharePath ?: return null

        try {
            val client = smbConnectionPool.getConnection(
                serverId = serverId,
                host = "", port = 445, username = "", password = "", shareName = "",
            )
            val parentDir = sharePath.substringBeforeLast('/')
            val baseName = sharePath.substringAfterLast('/').substringBeforeLast('.')

            val files = client.listFiles(parentDir).getOrNull() ?: return null
            val lrcFile = files.find {
                !it.isDirectory &&
                it.name.endsWith(".lrc", ignoreCase = true) &&
                it.name.substringBeforeLast('.').equals(baseName, ignoreCase = true)
            } ?: return null

            val stream = client.openFile(lrcFile.path).getOrThrow()
            val content = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parseLyricsContent(content)
        } catch (_: Exception) {
            return null
        }
    }

    /** 从 SMB 文件读取内嵌歌词 */
    private suspend fun findSmbEmbeddedLyrics(song: SongEntity): LyricsData? {
        // SMB 内嵌歌词需要读取文件头部，暂时跳过
        return null
    }

    /** 解析歌词文件 */
    private fun parseLyricsFile(file: File): LyricsData? {
        return try {
            val content = file.readText(Charsets.UTF_8)
            parseLyricsContent(content)
        } catch (_: Exception) {
            null
        }
    }

    /** 解析歌词内容（兼容 LRC 和纯文本） */
    private fun parseLyricsContent(content: String): LyricsData? {
        // 先尝试 LRC 解析
        val lrcData = LrcParser.parse(content)
        if (lrcData.lines.isNotEmpty()) return lrcData

        // 纯文本歌词
        return parsePlainText(content)
    }

    /** 解析纯文本歌词 */
    private fun parsePlainText(content: String): LyricsData? {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return null
        return LyricsData(
            lines = lines.mapIndexed { index, text ->
                com.hezi.juyumao.player.audio.LyricLine(
                    timeMs = index.toLong() * 5000, // 每行间隔 5 秒
                    text = text.trim(),
                )
            },
        )
    }
}
