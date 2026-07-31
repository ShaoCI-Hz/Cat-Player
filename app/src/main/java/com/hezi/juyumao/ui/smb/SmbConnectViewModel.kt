package com.hezi.juyumao.ui.smb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.entity.ServerEntity
import com.hezi.juyumao.data.remote.discovery.DiscoveredServer
import com.hezi.juyumao.data.remote.discovery.SmbDiscovery
import com.hezi.juyumao.data.remote.smb.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmbConnectUiState(
    val isScanning: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val savedServers: List<ServerEntity> = emptyList(),
    val connectionState: SmbConnectionState = SmbConnectionState.Disconnected,
    val errorMessage: String? = null,
    val connectSuccess: Boolean = false,
    val availableShares: List<String> = emptyList(), // 连接后发现的共享列表
)

@HiltViewModel
class SmbConnectViewModel @Inject constructor(
    private val serverDao: ServerDao,
    private val connectionPool: SmbConnectionPool,
) : ViewModel() {

    private val discovery = SmbDiscovery()

    private val _uiState = MutableStateFlow(SmbConnectUiState())
    val uiState: StateFlow<SmbConnectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            serverDao.getAllServers().collect { servers ->
                _uiState.value = _uiState.value.copy(savedServers = servers)
            }
        }
    }

    fun discover() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, discoveredServers = emptyList())
            val result = discovery.discover()
            result.fold(
                onSuccess = { servers ->
                    _uiState.value = _uiState.value.copy(isScanning = false, discoveredServers = servers)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isScanning = false, errorMessage = "扫描失败: ${e.message}")
                },
            )
        }
    }

    fun connect(ip: String, port: Int, username: String, password: String, shareName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionState = SmbConnectionState.Connecting,
                errorMessage = null,
                connectSuccess = false,
                availableShares = emptyList(),
            )

            // 先保存服务器
            val existing = _uiState.value.savedServers.find { it.ip == ip }
            val server = existing ?: ServerEntity(
                name = ip, ip = ip, port = port,
                username = username, password = password,
                shareName = shareName, autoConnect = true,
            )
            val serverId = if (existing != null) existing.id else serverDao.insert(server)

            try {
                connectionPool.getConnection(
                    serverId = serverId, host = ip, port = port,
                    username = username, password = password, shareName = shareName,
                )
                serverDao.update(server.copy(id = serverId, lastConnectedAt = System.currentTimeMillis()))
                _uiState.value = _uiState.value.copy(
                    connectionState = SmbConnectionState.Connected,
                    connectSuccess = true,
                )
            } catch (e: SmbConnectionException) {
                serverDao.updateConnectionError(serverId, e.message)
                _uiState.value = _uiState.value.copy(
                    connectionState = SmbConnectionState.Error(e.message ?: "连接失败"),
                    errorMessage = e.message,
                )
            } catch (e: Exception) {
                val msg = "连接失败: ${e.message}"
                serverDao.updateConnectionError(serverId, msg)
                _uiState.value = _uiState.value.copy(
                    connectionState = SmbConnectionState.Error(msg),
                    errorMessage = msg,
                )
            }
        }
    }

    fun connectWithShare(shareName: String) {
        // 用户从共享列表中选择了共享，重新连接
        val current = _uiState.value
        val server = current.savedServers.lastOrNull() ?: return
        connect(server.ip, server.port, server.username, server.password, shareName)
    }

    fun connectToDiscovered(server: DiscoveredServer) {
        // 发现的服务器先尝试匿名连接，获取共享列表
        connect(ip = server.host, port = server.port, username = "", password = "", shareName = "")
    }

    fun connectToSaved(server: ServerEntity) {
        connect(server.ip, server.port, server.username, server.password, server.shareName)
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            connectionPool.disconnect(server.id)
            serverDao.delete(server)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetConnectSuccess() {
        _uiState.value = _uiState.value.copy(connectSuccess = false)
    }

    override fun onCleared() {
        super.onCleared()
        discovery.stop()
    }
}
