package com.hezi.juyumao.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.crypto.decryptPassword
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.local.metadata.MetadataBatchProcessor
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import com.hezi.juyumao.data.repository.SettingsRepository
import com.hezi.juyumao.player.PlaybackStateHolder
import com.hezi.juyumao.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 自动重连状态 */
data class ReconnectState(
    val isReconnecting: Boolean = false,
    val message: String? = null,
    val success: Boolean? = null,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val playbackStateHolder: PlaybackStateHolder,
    private val serverDao: ServerDao,
    private val connectionPool: SmbConnectionPool,
    private val songDao: SongDao,
    private val metadataBatchProcessor: MetadataBatchProcessor,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val currentSong: StateFlow<SongEntity?> = playbackStateHolder.currentSong
    val artworkUri: StateFlow<String?> = playbackStateHolder.artworkUri
    val isPlaying: StateFlow<Boolean> = playbackStateHolder.isPlaying

    private val _reconnectState = MutableStateFlow(ReconnectState())
    val reconnectState: StateFlow<ReconnectState> = _reconnectState.asStateFlow()

    init {
        // 启动时自动重连已保存的 SMB 服务器
        autoReconnectSavedServers()
    }

    /** 清空重连提示（提示消失后调用） */
    fun clearReconnectMessage() {
        _reconnectState.value = _reconnectState.value.copy(message = null)
    }

    private fun autoReconnectSavedServers() {
        viewModelScope.launch {
            _reconnectState.value = ReconnectState(isReconnecting = true)
            try {
                val serverList = serverDao.getAutoConnectServers().first()
                var anyConnected = false
                val connectedNames = mutableListOf<String>()
                for (server in serverList) {
                    val decrypted = server.decryptPassword()
                    val existing = connectionPool.getExistingConnection(server.id)
                    if (existing == null) {
                        // 最多重试 3 次，间隔递增
                        var success = false
                        repeat(3) { attempt ->
                            if (!success) {
                                try {
                                    Log.d("AppVM", "自动重连(${attempt + 1}/3): ${decrypted.ip}:${decrypted.port}")
                                    connectionPool.getConnection(
                                        serverId = decrypted.id,
                                        host = decrypted.ip,
                                        port = decrypted.port,
                                        username = decrypted.username,
                                        password = decrypted.password,
                                        shareName = decrypted.effectiveShareName,
                                    )
                                    Log.d("AppVM", "重连成功: ${decrypted.ip}")
                                    success = true
                                    anyConnected = true
                                    connectedNames.add(decrypted.ip)
                                } catch (e: Exception) {
                                    Log.e("AppVM", "重连失败(${attempt + 1}/3): ${decrypted.ip}", e)
                                    kotlinx.coroutines.delay((attempt + 1) * 2000L)
                                }
                            }
                        }
                    } else {
                        anyConnected = true
                        connectedNames.add(decrypted.ip)
                    }
                }

                // 重连成功且存在未缓存的 NAS 歌曲时，触发批量元数据缓存
                if (anyConnected) {
                    val smbSongs = songDao.getAllSongs().first().filter { it.source == "SMB" }
                    val needCache = smbSongs.filter { !it.isMetadataCached() }
                    if (needCache.isNotEmpty()) {
                        Log.d("AppVM", "触发批量缓存 ${needCache.size} 首（已缓存 ${smbSongs.size - needCache.size} 首跳过）")
                        metadataBatchProcessor.processSongs(needCache)
                    }
                    val summary = if (connectedNames.isEmpty()) "已连接 NAS" else "已连接 NAS: ${connectedNames.joinToString("、")}"
                    _reconnectState.value = ReconnectState(isReconnecting = false, message = summary, success = true)
                } else {
                    _reconnectState.value = ReconnectState(
                        isReconnecting = false,
                        message = "自动连接失败，请手动连接 NAS",
                        success = false,
                    )
                }
            } catch (e: Exception) {
                Log.e("AppVM", "自动重连异常", e)
                _reconnectState.value = ReconnectState(
                    isReconnecting = false,
                    message = "自动连接异常",
                    success = false,
                )
            }
        }
    }

    fun togglePlay() {
        val exoPlayer = playbackStateHolder.getExoPlayer() ?: return
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            playbackStateHolder.updatePlaying(false)
        } else {
            exoPlayer.play()
            playbackStateHolder.updatePlaying(true)
        }
    }
}

/**
 * 判断歌曲元数据是否已缓存
 * 只有封面已缓存才算完成（封面是浏览列表展示的关键，歌手缺失会重新提取）
 */
private fun SongEntity.isMetadataCached(): Boolean =
    !albumArtUri.isNullOrEmpty()
