package com.hezi.juyumao.player

import com.hezi.juyumao.domain.model.RepeatMode
import com.hezi.juyumao.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放队列管理（不通过 Hilt 注入，由 PlaybackController 直接创建）
 */
class PlaybackQueue {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleOrder = MutableStateFlow<List<Int>>(emptyList())

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _songs.value = songs
        _currentIndex.value = startIndex
        if (songs.isNotEmpty()) {
            _shuffleOrder.value = songs.indices.shuffled()
        }
    }

    fun currentSong(): Song? {
        val idx = _currentIndex.value
        return if (idx in _songs.value.indices) _songs.value[idx] else null
    }

    fun next(repeatMode: RepeatMode, shuffle: Boolean): Song? {
        val songs = _songs.value
        if (songs.isEmpty()) return null

        val nextIndex = when (repeatMode) {
            RepeatMode.ONE -> _currentIndex.value
            RepeatMode.ALL -> {
                if (shuffle) {
                    val shuffleIdx = _shuffleOrder.value.indexOf(_currentIndex.value)
                    // 修复: indexOf 返回 -1 时重建 shuffle order
                    if (shuffleIdx == -1) {
                        _shuffleOrder.value = songs.indices.shuffled()
                        (_currentIndex.value + 1) % songs.size
                    } else {
                        _shuffleOrder.value[(shuffleIdx + 1) % songs.size]
                    }
                } else {
                    (_currentIndex.value + 1) % songs.size
                }
            }
            RepeatMode.OFF -> {
                val next = _currentIndex.value + 1
                if (next >= songs.size) return null
                if (shuffle) {
                    val shuffleIdx = _shuffleOrder.value.indexOf(_currentIndex.value)
                    if (shuffleIdx == -1 || shuffleIdx + 1 >= songs.size) return null
                    _shuffleOrder.value[shuffleIdx + 1]
                } else next
            }
        }

        _currentIndex.value = nextIndex
        return currentSong()
    }

    fun previous(repeatMode: RepeatMode, shuffle: Boolean = false): Song? {
        val songs = _songs.value
        if (songs.isEmpty()) return null

        _currentIndex.value = when (repeatMode) {
            RepeatMode.ALL -> {
                if (shuffle) {
                    val shuffleIdx = _shuffleOrder.value.indexOf(_currentIndex.value)
                    if (shuffleIdx == -1) {
                        _shuffleOrder.value = songs.indices.shuffled()
                        songs.size - 1
                    } else {
                        val prevIdx = if (shuffleIdx <= 0) songs.size - 1 else shuffleIdx - 1
                        _shuffleOrder.value[prevIdx]
                    }
                } else {
                    (_currentIndex.value - 1 + songs.size) % songs.size
                }
            }
            else -> maxOf(0, _currentIndex.value - 1)
        }
        return currentSong()
    }

    fun remove(index: Int) {
        val songs = _songs.value.toMutableList()
        if (index !in songs.indices) return
        songs.removeAt(index)
        _songs.value = songs
        // 修复: 删除后重建 shuffle 索引
        _shuffleOrder.value = songs.indices.shuffled()
        if (_currentIndex.value >= songs.size) {
            _currentIndex.value = maxOf(0, songs.size - 1)
        }
    }

    fun clear() {
        _songs.value = emptyList()
        _currentIndex.value = -1
        _shuffleOrder.value = emptyList()
    }
}
