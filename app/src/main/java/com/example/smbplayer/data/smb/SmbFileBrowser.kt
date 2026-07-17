package com.example.smbplayer.data.smb

import com.hierynomus.msfscc.FileAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbFileBrowser @Inject constructor(
    private val connectionManager: SmbConnectionManager
) {
    private val audioExtensions = setOf("mp3", "flac", "ogg", "wav", "aac", "wma", "opus", "m4a", "ape", "wv")

    suspend fun listFiles(path: String): List<SmbFileEntry> = withContext(Dispatchers.IO) {
        val share = connectionManager.activeShare
            ?: throw SmbNotConnectedException()

        val normalizedPath = path.trimStart('/').let { if (it.isEmpty()) "." else it }
        val entries = share.list(normalizedPath)

        entries
            .filter { it.fileName != "." && it.fileName != ".." }
            .map { info ->
                val isDir = (info.fileAttributes and com.hierynomus.msfscc.FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                SmbFileEntry(
                    name = info.fileName,
                    path = if (path == "." || path == "/" || path.isEmpty()) info.fileName
                           else "$path/${info.fileName}".trimStart('/'),
                    isDirectory = isDir,
                    size = if (isDir) 0 else info.endOfFile,
                    lastModified = info.lastWriteTime.toEpochMillis()
                )
            }
            .sortedWith(compareByDescending<SmbFileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "")
        return ext.lowercase() in audioExtensions
    }

    fun getInputStream(path: String): InputStream =
        connectionManager.openFileStream(path)

    fun getFileSize(path: String): Long =
        connectionManager.getFileSize(path)
}
