package com.example.smbplayer.data.smb

import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbFileBrowser @Inject constructor(
    private val connectionManager: SmbConnectionManager
) {
    private val audioExtensions = setOf(
        "mp3", "flac", "ogg", "wav", "aac", "wma", "opus", "m4a", "ape", "wv"
    )

    suspend fun listDirectory(path: String): List<SmbFileEntry> = withContext(Dispatchers.IO) {
        try {
            val ctx = connectionManager.run {
                // Get the SMB context from connection manager
                val field = SmbConnectionManager::class.java.getDeclaredField("smbContext")
                field.isAccessible = true
                field.get(this) as? jcifs.CIFSContext
            } ?: return@withContext emptyList()

            val smbFile = SmbFile(path, ctx)
            smbFile.listFiles().map { file ->
                SmbFileEntry(
                    name = file.name,
                    path = file.path,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L
                )
            }.sortedWith(compareBy<SmbFileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "")
        return ext.lowercase() in audioExtensions
    }

    fun getInputStream(path: String): InputStream {
        val ctx = connectionManager.run {
            val field = SmbConnectionManager::class.java.getDeclaredField("smbContext")
            field.isAccessible = true
            field.get(this) as? jcifs.CIFSContext
        } ?: throw SmbNotConnectedException()

        val smbFile = SmbFile(path, ctx)
        return smbFile.inputStream
    }

    fun getFileSize(path: String): Long {
        val ctx = connectionManager.run {
            val field = SmbConnectionManager::class.java.getDeclaredField("smbContext")
            field.isAccessible = true
            field.get(this) as? jcifs.CIFSContext
        } ?: return 0L

        return try {
            SmbFile(path, ctx).length()
        } catch (_: Exception) { 0L }
    }
}
