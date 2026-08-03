package com.hezi.juyumao.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.domain.model.Song
import com.hezi.juyumao.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    init {
        // 初始化时同步一次当前队列
        refresh()
    }

    private fun refresh() {
        _queue.value = playbackController.getQueue()
        _currentIndex.value = playbackController.getQueueIndex()
    }

    fun playAt(index: Int) {
        viewModelScope.launch {
            playbackController.playAt(index)
            refresh()
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            playbackController.clearQueue()
            refresh()
        }
    }
}
