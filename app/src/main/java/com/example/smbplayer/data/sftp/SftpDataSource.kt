package com.example.smbplayer.data.sftp

import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException
import java.io.InputStream
import java.util.EnumSet

/**
 * Media3 DataSource for SFTP streaming via SSHJ.
 */
class SftpDataSource(
    private val host: String,
    private val port: Int = 22,
    private val username: String,
    private val password: String,
    private val remotePath: String,
    private val fileSize: Long
) : BaseDataSource(/* isNetwork = */ true) {

    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null
    private var remoteFile: RemoteFile? = null
    private var fileInputStream: InputStream? = null
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        close()
        transferInitializing(dataSpec)

        try {
            sshClient = SSHClient().apply {
                addHostKeyVerifier("")
                connect(host, port)
                authPassword(username, password)
            }
            sftpClient = sshClient!!.newSFTPClient()

            val modes = EnumSet.of(OpenMode.READ)
            remoteFile = sftpClient!!.open(remotePath, modes)

            // Create input stream from remote file
            val rf = remoteFile!!
            fileInputStream = object : InputStream() {
                private var position = dataSpec.position

                override fun read(): Int {
                    val buf = ByteArray(1)
                    val read = read(buf, 0, 1)
                    return if (read <= 0) -1 else buf[0].toInt() and 0xFF
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val bytesRead = rf.read(position, b, off, len)
                    if (bytesRead > 0) position += bytesRead
                    return bytesRead
                }
            }

            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileSize - dataSpec.position
            }

            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
            close()
            throw IOException("SFTP open failed: ${e.message}", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val stream = fileInputStream ?: return C.RESULT_END_OF_INPUT
        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()
        val bytesRead = stream.read(buffer, offset, bytesToRead)

        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        bytesRemaining -= bytesRead.toLong()
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): android.net.Uri? = android.net.Uri.parse("sftp://$host:$port$remotePath")

    override fun close() {
        transferEnded()
        try { fileInputStream?.close() } catch (_: Exception) {}
        try { remoteFile?.close() } catch (_: Exception) {}
        try { sftpClient?.close() } catch (_: Exception) {}
        try { sshClient?.disconnect() } catch (_: Exception) {}
        fileInputStream = null
        remoteFile = null
        sftpClient = null
        sshClient = null
        bytesRemaining = 0
    }
}
