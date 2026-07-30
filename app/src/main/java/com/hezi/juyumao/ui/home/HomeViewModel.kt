package com.hezi.juyumao.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val playCount: Long = 0,
    val totalSize: Long = 0L,
    val isSmbConnected: Boolean = false,
    val isScanning: Boolean = false,
    val scanMessage: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        // 收集统计数据
        viewModelScope.launch {
            combine(
                musicRepository.getSongCount(),
                musicRepository.getTotalSize(),
                musicRepository.getAlbumCount(),
                musicRepository.getArtistCount(),
            ) { count, size, albums, artists ->
                _uiState.value.copy(
                    songCount = count,
                    totalSize = size ?: 0L,
                    albumCount = albums,
                    artistCount = artists,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun scanLocalMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanMessage = "扫描中...")
            val result = musicRepository.scanLocalMusic()
            result.fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        scanMessage = "扫描完成，找到 $count 首歌曲",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        scanMessage = "扫描失败: ${e.message}",
                    )
                },
            )
        }
    }
}
