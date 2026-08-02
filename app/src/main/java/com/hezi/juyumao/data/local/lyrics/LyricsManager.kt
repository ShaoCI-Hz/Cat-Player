package com.hezi.juyumao.data.local.lyrics

import android.content.Context
import com.hezi.juyumao.data.local.crypto.decryptPassword
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.local.metadata.MetadataExtractor
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import com.hezi.juyumao.player.audio.LrcParser
import com.hezi.juyumao.player.audio.LyricsData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataExtractor: MetadataExtractor,
    private val smbConnectionPool: SmbConnectionPool,
    private val serverDao: ServerDao,
) {

    suspend fun getLyrics(song: SongEntity): LyricsData? = withContext(Dispatchers.IO) {
        when (song.source) {
            "LOCAL" -> getLyricsLocal(song.filePath)
            "SMB" -> getLyricsSmb(song)
            else -> null
        }
    }

    private suspend fun getLyricsLocal(filePath: String): LyricsData? {
        findExternalLrc(filePath)?.let { return it }
        findEmbeddedLyrics(filePath)?.let { return it }
        findExternalTxt(filePath)?.let { return it }
        return null
    }

    private suspend fun getLyricsSmb(song: SongEntity): LyricsData? {
        findSmbLrc(song)?.let { return it }
        findSmbEmbeddedLyrics(song)?.let { return it }
        return null
    }

    private suspend fun findSmbEmbeddedLyrics(song: SongEntity): LyricsData? {
        try {
            val serverId = song.smbServerId ?: return null
            val server = serverDao.getServerById(serverId)?.decryptPassword() ?: return null
            val client = smbConnectionPool.getConnection(
                serverId = server.id,
                host = server.ip,
                port = server.port,
                username = server.username,
                password = server.password,
                shareName = server.effectiveShareName,
            )

            val sharePath = song.smbSharePath ?: return null
            val ext = sharePath.substringAfterLast('.', "").ifBlank { "bin" }
            // 必须保留原始扩展名，jaudiotagger 按扩展名选择解析器，.tmp 会解析失败读不到歌词
            val tempFile = File(context.cacheDir, "smb_lyr_${song.id}_${System.currentTimeMillis()}.$ext")
            try {
                client.openFile(sharePath).getOrThrow().use { input ->
                    val output = tempFile.outputStream()
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    val maxRead = 8L * 1024 * 1024
                    while (total < maxRead) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        total += n
                        if (total >= maxRead) break
                    }
                    output.close()
                }
                val meta = metadataExtractor.extract(tempFile.absolutePath)
                val lyricsText = meta.embeddedLyrics ?: return null
                return parseLyricsContent(lyricsText)
            } finally {
                tempFile.delete()
            }
        } catch (_: Exception) {
            return null
        }
    }

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

    private suspend fun findEmbeddedLyrics(filePath: String): LyricsData? {
        val meta = metadataExtractor.extract(filePath)
        val lyricsText = meta.embeddedLyrics ?: return null
        return parseLyricsContent(lyricsText)
    }

    private suspend fun findSmbLrc(song: SongEntity): LyricsData? {
        val serverId = song.smbServerId ?: return null
        val sharePath = song.smbSharePath ?: return null
        val server = serverDao.getServerById(serverId)?.decryptPassword() ?: return null

        try {
            val client = smbConnectionPool.getConnection(
                serverId = serverId,
                host = server.ip,
                port = server.port,
                username = server.username,
                password = server.password,
                shareName = server.effectiveShareName,
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

    private fun parseLyricsFile(file: File): LyricsData? {
        return try {
            parseLyricsContent(file.readText(Charsets.UTF_8))
        } catch (_: Exception) { null }
    }

    private fun parseLyricsContent(content: String): LyricsData? {
        val lrcData = LrcParser.parse(content)
        if (lrcData.lines.isNotEmpty()) return lrcData
        return parsePlainText(content)
    }

    private fun parsePlainText(content: String): LyricsData? {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return null
        // 纯文本歌词不分配假时间戳，返回单行时间 0
        return LyricsData(
            lines = lines.map { text ->
                com.hezi.juyumao.player.audio.LyricLine(timeMs = 0L, text = text.trim())
            },
        )
    }
}
