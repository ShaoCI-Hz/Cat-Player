package com.hezi.juyumao.ui.smb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.entity.ServerEntity
import com.hezi.juyumao.data.remote.discovery.DiscoveredServer
import com.hezi.juyumao.data.remote.discovery.SmbDiscovery
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import com.hezi.juyumao.data.remote.smb.SmbConnectionState
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
        // 加载已保存的服务器
        viewModelScope.launch {
            serverDao.getAllServers().collect { servers ->
                _uiState.value = _uiState.value.copy(savedServers = servers)
            }
        }
    }

    /** 自动发现局域网 SMB 设备 */
    fun discover() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, discoveredServers = emptyList())
            val result = discovery.discover()
            result.fold(
                onSuccess = { servers ->
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        discoveredServers = servers,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        errorMessage = "扫描失败: ${e.message}",
                    )
                },
            )
        }
    }

    /** 连接到 SMB 服务器 */
    fun connect(ip: String, port: Int, username: String, password: String, shareName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionState = SmbConnectionState.Connecting,
                errorMessage = null,
                connectSuccess = false,
            )

            // 保存服务器信息
            val server = ServerEntity(
                name = ip,
                ip = ip,
                port = port,
                username = username,
                password = password,
                shareName = shareName,
                autoConnect = true,
            )
            val serverId = serverDao.insert(server)

            // 尝试连接
            try {
                connectionPool.getConnection(
                    serverId = serverId,
                    host = ip,
                    port = port,
                    username = username,
                    password = password,
                    shareName = shareName,
                )
                serverDao.update(server.copy(id = serverId, lastConnectedAt = System.currentTimeMillis()))
                _uiState.value = _uiState.value.copy(
                    connectionState = SmbConnectionState.Connected,
                    connectSuccess = true,
                )
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("connect", true) == true -> "无法连接到 NAS，请检查 Wi-Fi 和 NAS 是否在同一网络"
                    e.message?.contains("auth", true) == true -> "用户名或密码错误"
                    e.message?.contains("timeout", true) == true -> "连接超时，NAS 可能离线"
                    else -> "连接失败: ${e.message}"
                }
                serverDao.updateConnectionError(serverId, errorMsg)
                _uiState.value = _uiState.value.copy(
                    connectionState = SmbConnectionState.Error(errorMsg),
                    errorMessage = errorMsg,
                )
            }
        }
    }

    /** 连接到自动发现的服务器 */
    fun connectToDiscovered(server: DiscoveredServer) {
        connect(
            ip = server.host,
            port = server.port,
            username = "",
            password = "",
            shareName = "", // 需要用户输入共享名
        )
    }

    /** 连接到已保存的服务器 */
    fun connectToSaved(server: ServerEntity) {
        connect(
            ip = server.ip,
            port = server.port,
            username = server.username,
            password = server.password,
            shareName = server.shareName,
        )
    }

    /** 删除已保存的服务器 */
    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            connectionPool.disconnect(server.id)
            serverDao.delete(server)
        }
    }

    /** 清除错误信息 */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** 重置连接成功状态 */
    fun resetConnectSuccess() {
        _uiState.value = _uiState.value.copy(connectSuccess = false)
    }

    override fun onCleared() {
        super.onCleared()
        discovery.stop()
    }
}
