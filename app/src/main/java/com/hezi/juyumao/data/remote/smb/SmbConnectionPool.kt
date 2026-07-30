package com.hezi.juyumao.data.remote.smb

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class PooledConnection(
    val client: SmbClientWrapper,
    val serverId: Long,
    val lastAccessed: Long = System.currentTimeMillis(),
)

class SmbConnectionPool @Inject constructor(
    private val smbClient: SmbClientWrapper,
) {
    private val maxConnections = 3
    private val idleTimeoutMs = 60_000L
    private val connections = ConcurrentHashMap<Long, PooledConnection>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<SmbConnectionState>(SmbConnectionState.Disconnected)
    val connectionState: StateFlow<SmbConnectionState> = _connectionState

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
    ): SmbClientWrapper {
        connections[serverId]?.let { pooled ->
            if (pooled.client.isConnected()) {
                connections[serverId] = pooled.copy(lastAccessed = System.currentTimeMillis())
                return pooled.client
            }
        }

        if (connections.size >= maxConnections) {
            evictOldest()
        }

        _connectionState.value = SmbConnectionState.Connecting
        val result = smbClient.connect(host, port, username, password, shareName)
        if (result.isSuccess) {
            connections[serverId] = PooledConnection(smbClient, serverId)
            _connectionState.value = SmbConnectionState.Connected
            return smbClient
        } else {
            _connectionState.value = SmbConnectionState.Error(result.exceptionOrNull()?.message ?: "连接失败")
            throw result.exceptionOrNull() ?: Exception("连接失败")
        }
    }

    fun disconnect(serverId: Long) {
        connections.remove(serverId)?.client?.disconnect()
        if (connections.isEmpty()) {
            _connectionState.value = SmbConnectionState.Disconnected
        }
    }

    fun disconnectAll() {
        connections.values.forEach { it.client.disconnect() }
        connections.clear()
        _connectionState.value = SmbConnectionState.Disconnected
    }

    private fun cleanupIdleConnections() {
        val now = System.currentTimeMillis()
        connections.entries.removeIf { (_, pooled) ->
            if (now - pooled.lastAccessed > idleTimeoutMs) {
                pooled.client.disconnect()
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
