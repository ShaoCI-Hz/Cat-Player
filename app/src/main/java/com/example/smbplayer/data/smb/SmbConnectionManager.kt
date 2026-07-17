package com.example.smbplayer.data.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbConnectionManager @Inject constructor() {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null

    val activeShare: DiskShare?
        get() = if (_connectionState.value == ConnectionState.Connected) diskShare else null

    private var healthScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastConfig: SmbConfig? = null
    private var healthCheckRunning = false

    suspend fun connect(config: SmbConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting
            disconnect()
            withTimeout(15_000) {
                val cli = SMBClient()
                client = cli
                val conn = cli.connect(config.host)
                connection = conn
                val authContext = AuthenticationContext(
                    config.username,
                    config.password.toCharArray(),
                    config.domain.ifEmpty { null }
                )
                val sess = conn.authenticate(authContext)
                session = sess
                diskShare = sess.connectShare(config.shareName) as? DiskShare
                    ?: throw IllegalStateException("共享 ${config.shareName} 不是磁盘共享类型")
            }

            _connectionState.value = ConnectionState.Connected
            lastConfig = config
            startHealthCheck()
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect(setState = false)
            _connectionState.value = ConnectionState.Error
            Result.failure(e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            disconnect(setState = false)
            _connectionState.value = ConnectionState.Error
            throw e
        }
    }

    suspend fun disconnect(setState: Boolean = true) = withContext(Dispatchers.IO) {
        try { diskShare?.close() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { connection?.close() } catch (_: Exception) {}
        try { client?.close() } catch (_: Exception) {}

        diskShare = null
        session = null
        connection = null
        client = null
        if (setState) _connectionState.value = ConnectionState.Disconnected
    }

    fun openFileStream(path: String): InputStream {
        val share = diskShare ?: throw SmbNotConnectedException()
        val file = share.openFile(
            path,
            setOf(AccessMask.GENERIC_READ),
            null,
            null,
            null,
            null
        )
        return file.inputStream
    }

    fun getFileSize(path: String): Long {
        val share = diskShare ?: throw SmbNotConnectedException()
        val info = share.getFileInformation(path)
        return info.standardInformation.endOfFile
    }

    private fun startHealthCheck() {
        if (healthCheckRunning) return
        healthCheckRunning = true
        if (healthScope.coroutineContext[kotlinx.coroutines.Job]?.isActive != true) healthScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        healthScope.launch {
            while (healthCheckRunning) {
                delay(30_000)
                try {
                    val alive = diskShare?.folderExists(".") ?: false
                    if (!alive) {
                        _connectionState.value = ConnectionState.Error
                        lastConfig?.let { connect(it) }
                    }
                } catch (_: Exception) {
                    _connectionState.value = ConnectionState.Error
                    lastConfig?.let { connect(it) }
                }
            }
        }
    }

    fun stopHealthCheck() {
        healthCheckRunning = false
        healthScope.cancel()
        lastConfig = null
    }
}
