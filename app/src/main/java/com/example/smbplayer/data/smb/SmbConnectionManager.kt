package com.example.smbplayer.data.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
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
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AutoCloseable wrapper that closes both the SMB File and its InputStream.
 * Prevents server-side file handle leaks.
 */
class SmbFileStream(
    private val file: File,
    private val stream: InputStream
) : InputStream() {
    override fun read(): Int = stream.read()
    override fun read(b: ByteArray): Int = stream.read(b)
    override fun read(b: ByteArray, off: Int, len: Int): Int = stream.read(b, off, len)
    override fun available(): Int = stream.available()
    override fun close() {
        try { stream.close() } catch (_: Exception) {}
        try { file.close() } catch (_: Exception) {}
    }
}

@Singleton
class SmbConnectionManager @Inject constructor() {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    @Volatile private var client: SMBClient? = null
    @Volatile private var connection: Connection? = null
    @Volatile private var session: Session? = null
    @Volatile private var diskShare: DiskShare? = null

    // Generation counter: increments on each reconnect so active streams can detect stale connections
    private val connectionGeneration = AtomicInteger(0)

    val activeShare: DiskShare?
        get() = if (_connectionState.value == ConnectionState.Connected) diskShare else null

    fun currentGeneration(): Int = connectionGeneration.get()

    private var healthScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var lastConfig: SmbConfig? = null
    private val healthCheckRunning = AtomicBoolean(false)
    private val connectMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun connect(config: SmbConfig): Result<Unit> = withContext(Dispatchers.IO) {
        // BUG-SMB-01 fix: Prevent concurrent connect/disconnect
        connectMutex.lock()
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
        connectionGeneration.incrementAndGet() // invalidate any active streams
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

    /**
     * Opens an SMB file stream wrapped in SmbFileStream (AutoCloseable).
     * The wrapper ensures both the File handle and InputStream are closed together.
     */
    fun openFileStream(path: String): SmbFileStream {
        val share = diskShare ?: throw SmbNotConnectedException()
        val file = share.openFile(
            path,
            setOf(AccessMask.GENERIC_READ),
            null, null, null, null
        )
        return SmbFileStream(file, file.inputStream)
    }

    fun getFileSize(path: String): Long {
        val share = diskShare ?: throw SmbNotConnectedException()
        val info = share.getFileInformation(path)
        return info.standardInformation.endOfFile
    }

    private fun startHealthCheck() {
        if (healthCheckRunning.getAndSet(true)) return
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
                        // Exponential-ish backoff: wait extra before reconnecting
                        delay(10_000)
                        lastConfig?.let { connect(it) }
                    }
                } catch (_: Exception) {
                    _connectionState.value = ConnectionState.Error
                    delay(10_000)
                    lastConfig?.let { connect(it) }
                }
            }
        }
    }

    fun stopHealthCheck() {
        healthCheckRunning.set(false)
        healthScope.cancel()
        lastConfig = null
    }
}
