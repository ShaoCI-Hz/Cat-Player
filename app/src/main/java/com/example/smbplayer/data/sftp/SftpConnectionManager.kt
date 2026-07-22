package com.example.smbplayer.data.sftp

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SftpFileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long
)

@Singleton
class SftpConnectionManager @Inject constructor() {
    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null
    private var isConnected = false
    private var currentHost = ""

    suspend fun connect(host: String, port: Int = 22, username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            sshClient = SSHClient().apply {
                // Accept all host keys (for simplicity)
                addHostKeyVerifier("")
                connect(host, port)
                authPassword(username, password)
            }
            sftpClient = sshClient!!.newSFTPClient()
            isConnected = true
            currentHost = host
            true
        } catch (_: Exception) {
            isConnected = false
            false
        }
    }

    fun disconnect() {
        try { sftpClient?.close() } catch (_: Exception) {}
        try { sshClient?.disconnect() } catch (_: Exception) {}
        sftpClient = null
        sshClient = null
        isConnected = false
        currentHost = ""
    }

    suspend fun listDirectory(path: String): List<SftpFileEntry> = withContext(Dispatchers.IO) {
        try {
            val client = sftpClient ?: return@withContext emptyList()
            client.ls(path).map { info ->
                SftpFileEntry(
                    path = "${path.trimEnd('/')}/${info.name}",
                    name = info.name,
                    isDirectory = info.isDirectory,
                    size = info.attributes.size
                )
            }.filter { !it.name.startsWith(".") }
            .sortedWith(compareBy<SftpFileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getFileSize(path: String): Long = withContext(Dispatchers.IO) {
        try {
            sftpClient?.stat(path)?.size ?: 0L
        } catch (_: Exception) { 0L }
    }

    fun isConnected(): Boolean = isConnected
    fun getCurrentHost(): String = currentHost
}
