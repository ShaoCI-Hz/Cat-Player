package com.hezi.juyumao.data.remote.smb

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class PooledConnection(
    val client: SmbClientWrapper,
    val serverId: Long,
    val lastAccessed: Long = System.currentTimeMillis(),
)

class SmbConnectionPool @Inject constructor() {
    private val maxConnections = 8
    private val idleTimeoutMs = 30 * 60 * 1000L  // 30 分钟空闲才断开
    private val connections = ConcurrentHashMap<Long, PooledConnection>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionMutex = Mutex()

    private val _connectionStates = ConcurrentHashMap<Long, MutableStateFlow<SmbConnectionState>>()

    fun connectionStateFor(serverId: Long): StateFlow<SmbConnectionState> =
        getMutableStateFor(serverId)

    private fun getMutableStateFor(serverId: Long): MutableStateFlow<SmbConnectionState> =
        _connectionStates.getOrPut(serverId) { MutableStateFlow(SmbConnectionState.Disconnected) }

    init {
        scope.launch {
            while (isActive) {
                cleanupIdleConnections()
                delay(30_000)
            }
        }
    }

    suspend fun getConnection(
        serverId: Long,
        host: String,
        port: Int,
        username: String,
        password: String,
        shareName: String,
        domain: String = "",
    ): SmbClientWrapper = connectionMutex.withLock {
        val stateFlow = getMutableStateFor(serverId)

        // 检查已有连接
        connections[serverId]?.let { pooled ->
            if (pooled.client.isConnected()) {
                connections[serverId] = pooled.copy(lastAccessed = System.currentTimeMillis())
                return pooled.client
            }
        }

        if (connections.size >= maxConnections) {
            evictOldest()
        }

        stateFlow.value = SmbConnectionState.Connecting

        Log.d("SmbPool", "创建新连接: $host:$port, share=$shareName")

        val client = SmbClientWrapper()
        try {
            client.connect(host, port, username, password, shareName, domain)
            connections[serverId] = PooledConnection(client, serverId)
            stateFlow.value = SmbConnectionState.Connected
            Log.d("SmbPool", "连接成功")
            client
        } catch (e: Exception) {
            Log.e("SmbPool", "连接失败", e)
            stateFlow.value = SmbConnectionState.Error(e.message ?: "连接失败")
            throw e
        }
    }

    fun getExistingConnection(serverId: Long): SmbClientWrapper? {
        return connections[serverId]?.takeIf { it.client.isConnected() }?.client
    }

    fun disconnect(serverId: Long) {
        connections.remove(serverId)?.client?.disconnect()
        _connectionStates[serverId]?.value = SmbConnectionState.Disconnected
    }

    fun disconnectAll() {
        connections.values.forEach { it.client.disconnect() }
        connections.clear()
        _connectionStates.values.forEach { it.value = SmbConnectionState.Disconnected }
    }

    fun close() {
        scope.cancel()
        disconnectAll()
    }

    private fun cleanupIdleConnections() {
        val now = System.currentTimeMillis()
        connections.entries.removeIf { (serverId, pooled) ->
            if (now - pooled.lastAccessed > idleTimeoutMs) {
                pooled.client.disconnect()
                _connectionStates[serverId]?.value = SmbConnectionState.Disconnected
                true
            } else false
        }
    }

    private fun evictOldest() {
        val oldest = connections.entries.minByOrNull { it.value.lastAccessed }
        oldest?.let {
            it.value.client.disconnect()
            connections.remove(it.key)
        }
    }
}

sealed class SmbConnectionState {
    data object Disconnected : SmbConnectionState()
    data object Connecting : SmbConnectionState()
    data object Connected : SmbConnectionState()
    data class Error(val message: String) : SmbConnectionState()
}
