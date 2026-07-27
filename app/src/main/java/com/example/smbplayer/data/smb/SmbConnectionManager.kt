package com.example.smbplayer.data.smb

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbConnectionManager @Inject constructor() {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var smbContext: CIFSContext? = null
    private var currentShare: String = ""
    private val connectionGeneration = AtomicInteger(0)

    private var healthScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var lastConfig: SmbConfig? = null
    private val healthCheckRunning = AtomicBoolean(false)
    private val connectMutex = kotlinx.coroutines.sync.Mutex()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    suspend fun connect(config: SmbConfig): Result<Unit> = withContext(Dispatchers.IO) {
        connectMutex.lock()
        try {
            _connectionState.value = ConnectionState.Connecting
            disconnect()

            withTimeout(15_000) {
                // Configure jcifs-ng
                val props = Properties().apply {
                    put("jcifs.smb.client.minVersion", "SMB202")
                    put("jcifs.smb.client.maxVersion", "SMB311")
                    put("jcifs.smb.client.responseTimeout", "10000")
                    put("jcifs.smb.client.soTimeout", "10000")
                }

                val baseContext = BaseContext(PropertyConfiguration(props))

                // Authentication
                val authContext = if (config.username.isEmpty()) {
                    baseContext.withGuestCrendentials()
                } else {
                    val auth = NtlmPasswordAuthenticator(
                        config.domain.ifEmpty { null },
                        config.username,
                        config.password
                    )
                    baseContext.withCredentials(auth)
                }

                smbContext = authContext
                currentShare = config.shareName

                // Test connection by listing root
                val shareUrl = "smb://${config.host}/${config.shareName}/"
                val testFile = SmbFile(shareUrl, authContext)
                testFile.exists() // This will throw if connection fails

                connectionGeneration.incrementAndGet()
                _connectionState.value = ConnectionState.Connected
                _isConnected.value = true
                lastConfig = config
                startHealthCheck()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _connectionState.value = ConnectionState.Error
            _isConnected.value = false
            Result.failure(e)
        } finally {
            connectMutex.unlock()
        }
    }

    suspend fun disconnect(setState: Boolean = true) = withContext(Dispatchers.IO) {
        connectionGeneration.incrementAndGet()
        stopHealthCheck()
        smbContext = null
        currentShare = ""
        if (setState) {
            _connectionState.value = ConnectionState.Disconnected
            _isConnected.value = false
        }
    }

    fun openFileStream(path: String): SmbFileStream {
        val ctx = smbContext ?: throw SmbNotConnectedException()
        val smbFile = SmbFile(path, ctx)
        if (!smbFile.exists()) throw SmbNotConnectedException()
        val stream = SmbFileInputStream(smbFile)
        return SmbFileStream(smbFile, stream)
    }

    fun getFileSize(path: String): Long {
        val ctx = smbContext ?: return 0L
        return try {
            val smbFile = SmbFile(path, ctx)
            smbFile.length()
        } catch (_: Exception) { 0L }
    }

    fun isConnected(): Boolean = _isConnected.value

    private fun startHealthCheck() {
        if (healthCheckRunning.get()) return
        healthCheckRunning.set(true)
        healthScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        healthScope.launch {
            while (healthCheckRunning.get()) {
                delay(30_000)
                try {
                    val ctx = smbContext ?: continue
                    val testFile = SmbFile("smb://${lastConfig?.host}/${lastConfig?.shareName}/", ctx)
                    if (!testFile.exists()) {
                        _connectionState.value = ConnectionState.Error
                        _isConnected.value = false
                        delay(10_000)
                        lastConfig?.let { connect(it) }
                    }
                } catch (_: Exception) {
                    _connectionState.value = ConnectionState.Error
                    _isConnected.value = false
                }
            }
        }
    }

    private fun stopHealthCheck() {
        healthCheckRunning.set(false)
        healthScope.cancel()
    }
}

data class SmbFileStream(
    private val smbFile: SmbFile,
    val inputStream: InputStream
) : AutoCloseable {
    override fun close() {
        try { inputStream.close() } catch (_: Exception) {}
    }
}
