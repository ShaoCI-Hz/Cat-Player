package com.example.smbplayer.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smbplayer.data.settings.SettingsRepository
import com.example.smbplayer.data.smb.ConnectionState
import com.example.smbplayer.data.smb.SmbConfig
import com.example.smbplayer.data.smb.SmbConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val connectionManager: SmbConnectionManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host.asStateFlow()

    private val _shareName = MutableStateFlow("")
    val shareName: StateFlow<String> = _shareName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            connectionState.collect { state ->
                _isConnected.value = state == ConnectionState.Connected
            }
        }
        viewModelScope.launch {
            settingsRepository.lastConnection.collect { triple ->
                if (triple != null) {
                    _host.value = triple.first
                    _shareName.value = triple.second
                    _username.value = triple.third
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.lastPassword.collect { pwd ->
                _password.value = pwd
            }
        }
    }

    fun updateHost(value: String) { _host.value = value }
    fun updateShareName(value: String) { _shareName.value = value }
    fun updateUsername(value: String) { _username.value = value }
    fun updatePassword(value: String) { _password.value = value }

    fun connect() {
        val hostValue = _host.value.trim()
        val shareValue = _shareName.value.trim()

        if (hostValue.isEmpty() || shareValue.isEmpty()) {
            _errorMessage.value = "请输入服务器地址和共享名称"
            return
        }

        _errorMessage.value = null
        viewModelScope.launch {
            val config = SmbConfig(
                host = hostValue,
                shareName = shareValue,
                username = _username.value.trim(),
                password = _password.value
            )
            val result = connectionManager.connect(config)
            result.onSuccess {
                settingsRepository.saveLastConnectionWithPassword(hostValue, shareValue, _username.value.trim(), _password.value)
            }.onFailure { e ->
                _errorMessage.value = "连接失败: ${e.localizedMessage ?: "未知错误"}"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
