package com.example.smbplayer.data.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
    private val connectionGeneration = AtomicInteger(0)

    val activeShare: DiskShare?
        get() = if (_connectionState.value == ConnectionState.Connected) diskShare else null

    private var healthScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var lastConfig: SmbConfig? = null
    private val healthCheckRunning = AtomicBoolean(false)
    private val connectMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun connect(config: SmbConfig): Result<Unit> = withContext(Dispatchers.IO) {
        connectMutex.lock()
        try {
            _connectionState.value = ConnectionState.Connecting
            disconnect()

            withTimeout(15_000) {
                // Create SMB client with encryption disabled
                val smbConfig = com.hierynomus.smbj.SmbConfig.builder()
                    .withEncryptData(false)
                    .build()
                val cli = SMBClient(smbConfig)
                client = cli

                val conn = cli.connect(config.host)
                connection = conn

                // Authentication - anonymous or credentials
                val authContext = if (config.username.isEmpty()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(
                        config.username,
                        config.password.toCharArray(),
                        config.domain.ifEmpty { null }
                    )
                }

                val sess = conn.authenticate(authContext)
                session = sess
                diskShare = sess.connectShare(config.shareName) as? DiskShare
                    ?: throw IllegalStateException("共享 ${config.shareName} 不是磁盘共享类型")
            }

            connectionGeneration.incrementAndGet()
            _connectionState.value = ConnectionState.Connected
            lastConfig = config
            startHealthCheck()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            disconnect(setState = false)
            _connectionState.value = ConnectionState.Error
            Result.failure(e)
        } finally {
            connectMutex.unlock()
        }
    }

    suspend fun disconnect(setState: Boolean = true) = withContext(Dispatchers.IO) {
        connectionGeneration.incrementAndGet()
        stopHealthCheck()
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

    fun openFileStream(path: String): SmbFileStream {
        val share = diskShare ?: throw SmbNotConnectedException()
        val file = share.openFile(
            path,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            null
        )
        return SmbFileStream(file, file.inputStream)
    }

    fun currentGeneration(): Int = connectionGeneration.get()

    private fun startHealthCheck() {
        if (healthCheckRunning.get()) return
        healthCheckRunning.set(true)
        if (healthScope.coroutineContext[Job]?.isActive != true) {
            healthScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }
        healthScope.launch {
            while (healthCheckRunning.get()) {
                delay(30_000)
                try {
                    val alive = diskShare?.folderExists(".") ?: false
                    if (!alive) {
                        _connectionState.value = ConnectionState.Error
                        delay(10_000)
                        lastConfig?.let { connect(it) }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopHealthCheck() {
        healthCheckRunning.set(false)
        healthScope.cancel()
    }
}

data class SmbFileStream(
    private val file: com.hierynomus.smbj.share.File,
    val inputStream: InputStream
) : AutoCloseable {
    override fun close() {
        try { inputStream.close() } catch (_: Exception) {}
        try { file.close() } catch (_: Exception) {}
    }
}
