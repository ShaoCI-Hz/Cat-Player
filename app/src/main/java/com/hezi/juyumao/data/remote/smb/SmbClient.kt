package com.hezi.juyumao.data.remote.smb

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

data class SmbFileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
)

class SmbClient @Inject constructor() {

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null

    suspend fun connect(
        host: String,
        port: Int = 445,
        username: String = "",
        password: String = "",
        shareName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            client = SMBClient()
            connection = client!!.connect(host, port)
            val ac = if (username.isEmpty()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(username, password.toCharArray(), null)
            }
            session = connection!!.authenticate(ac)
            share = session!!.connectShare(shareName) as DiskShare
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    suspend fun listFiles(path: String): Result<List<SmbFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val currentShare = share ?: return@withContext Result.failure(IllegalStateException("未连接"))
            val files = currentShare.list(path).mapNotNull { info ->
                if (info.fileName == "." || info.fileName == "..") return@mapNotNull null
                SmbFileInfo(
                    name = info.fileName,
                    path = if (path.endsWith("/")) "$path${info.fileName}" else "$path/${info.fileName}",
                    isDirectory = info.fileAttributes and 0x10 != 0L,
                    size = info.endOfFile,
                    lastModified = info.lastWriteTime.toEpochMillis(),
                )
            }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun openFile(path: String): Result<InputStream> = withContext(Dispatchers.IO) {
        try {
            val currentShare = share ?: return@withContext Result.failure(IllegalStateException("未连接"))
            val file = currentShare.openFile(
                path,
                setOf(com.hierynomus.msdtyp.AccessMask.FILE_READ_DATA),
                null,
                setOf(com.hierynomus.smb2.share.enums.SMB2ShareAccess.FILE_SHARE_READ),
                com.hierynomus.smb2.create.SMB2CreateDisposition.FILE_OPEN,
                null,
            )
            Result.success(file.inputStream)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean {
        return share != null && connection?.isConnected == true
    }

    fun disconnect() {
        try {
            share?.close()
            session?.close()
            connection?.close()
            client?.close()
        } catch (_: Exception) {}
        share = null
        session = null
        connection = null
        client = null
    }
}
