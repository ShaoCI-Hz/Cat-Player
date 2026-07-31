package com.hezi.juyumao.data.remote.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SmbFileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
)

class SmbClientWrapper @Inject constructor() {

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

            // 配置超时
            val config = SmbConfig.builder()
                .withTimeout(10, TimeUnit.SECONDS)
                .withSoTimeout(10, TimeUnit.SECONDS)
                .build()

            client = SMBClient(config)
            connection = client!!.connect(host, port)

            val ac = if (username.isEmpty()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(username, password.toCharArray(), "")
            }

            session = connection!!.authenticate(ac)

            if (shareName.isBlank()) {
                disconnect()
                return@withContext Result.failure(
                    SmbConnectionException("请输入共享名称（如 music、Media）")
                )
            }

            share = session!!.connectShare(shareName) as DiskShare
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            val errorMsg = when {
                e.message?.contains("connect", true) == true ||
                e.message?.contains("refused", true) == true ->
                    "无法连接到 $host:$port，请检查 IP 和 NAS 是否在同一网络"
                e.message?.contains("auth", true) == true ||
                e.message?.contains("logon", true) == true ->
                    "用户名或密码错误"
                e.message?.contains("timeout", true) == true ||
                e.message?.contains("timed out", true) == true ->
                    "连接超时（10秒），NAS 可能离线"
                e.message?.contains("name", true) == true ||
                e.message?.contains("STATUS_BAD_NETWORK_NAME", true) == true ->
                    "共享名 '$shareName' 不存在，请检查拼写"
                e.message?.contains("access", true) == true ||
                e.message?.contains("STATUS_ACCESS_DENIED", true) == true ->
                    "没有访问权限，请检查共享文件夹权限"
                else -> "连接失败: ${e.message}"
            }
            Result.failure(SmbConnectionException(errorMsg, e))
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
            val accessMask = EnumSet.of(AccessMask.FILE_READ_DATA)
            val shareAccess = java.util.EnumSet.of(
                com.hierynomus.mssmb2.SMB2ShareAccess.FILE_SHARE_READ
            )
            val file = currentShare.openFile(
                path, accessMask, null, shareAccess,
                com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OPEN, null,
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
        try { share?.close() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { connection?.close() } catch (_: Exception) {}
        try { client?.close() } catch (_: Exception) {}
        share = null
        session = null
        connection = null
        client = null
    }
}

class SmbConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)
