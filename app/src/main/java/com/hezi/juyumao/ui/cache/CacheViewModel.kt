package com.hezi.juyumao.ui.cache

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.cache.CacheManager
import com.hezi.juyumao.data.local.crypto.decryptPassword
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CacheUiState(
    val albumArtSize: Long = 0,
    val nasDownloadSize: Long = 0,
    val lyricsSize: Long = 0,
    val tempSize: Long = 0,
    val totalSize: Long = 0,
    val cachedNasSongs: List<CacheManager.CachedNasSong> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadMessage: String = "",
    val isClearing: Boolean = false,
    val lastAction: String? = null,
)

@HiltViewModel
class CacheViewModel @Inject constructor(
    private val cacheManager: CacheManager,
    private val songDao: SongDao,
    private val serverDao: ServerDao,
    private val smbConnectionPool: SmbConnectionPool,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CacheUiState())
    val uiState: StateFlow<CacheUiState> = _uiState.asStateFlow()

    init {
        refreshSizes()
    }

    fun refreshSizes() {
        viewModelScope.launch(Dispatchers.IO) {
            val sizes = cacheManager.getCacheSizes()
            val songs = cacheManager.getCachedNasSongs()
            _uiState.value = _uiState.value.copy(
                albumArtSize = sizes.albumArt,
                nasDownloadSize = sizes.nasDownloads,
                lyricsSize = sizes.lyrics,
                tempSize = sizes.temp,
                totalSize = sizes.total,
                cachedNasSongs = songs,
            )
        }
    }

    /**
     * 下载 NAS 歌曲到本地缓存
     */
    fun downloadNasSong(songId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, downloadMessage = "下载中...")
            try {
                val song = songDao.getById(songId) ?: return@launch
                if (song.source != "SMB" || song.smbServerId == null || song.smbSharePath == null) {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        lastAction = "仅 NAS 歌曲可下载",
                    )
                    return@launch
                }

                val server = serverDao.getServerById(song.smbServerId)?.decryptPassword()
                    ?: throw IllegalStateException("服务器不存在")

                val client = smbConnectionPool.getConnection(
                    serverId = server.id,
                    host = server.ip,
                    port = server.port,
                    username = server.username,
                    password = server.password,
                    shareName = server.effectiveShareName,
                )

                val result = withContext(Dispatchers.IO) {
                    client.openFile(song.smbSharePath!!).getOrThrow().use { input ->
                        cacheManager.saveNasSong(song.id, song.title.ifEmpty { "song" }, input)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    lastAction = "已下载: ${result.name}",
                )
                refreshSizes()
            } catch (e: Exception) {
                Log.e("CacheVM", "下载失败", e)
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    lastAction = "下载失败: ${e.message}",
                )
            }
        }
    }

    /**
     * 删除已下载的 NAS 歌曲
     */
    fun deleteNasSong(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            cacheManager.deleteNasSong(songId)
            _uiState.value = _uiState.value.copy(lastAction = "已删除缓存歌曲")
            refreshSizes()
        }
    }

    /**
     * 清除指定类型的缓存
     */
    fun clearCache(clearAlbumArt: Boolean, clearNas: Boolean, clearLyrics: Boolean, clearTemp: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isClearing = true)
            cacheManager.clearCache(clearAlbumArt, clearNas, clearLyrics, clearTemp)
            _uiState.value = _uiState.value.copy(isClearing = false, lastAction = "缓存已清除")
            refreshSizes()
        }
    }

    fun clearAllCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isClearing = true)
            cacheManager.clearAllCache()
            _uiState.value = _uiState.value.copy(isClearing = false, lastAction = "所有缓存已清除")
            refreshSizes()
        }
    }
}
