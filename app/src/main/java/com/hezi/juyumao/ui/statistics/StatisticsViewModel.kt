package com.hezi.juyumao.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 听歌报告 UI 状态 */
data class StatisticsUiState(
    val totalPlayCount: Long = 0,
    val weekPlayCount: Long = 0,
    val monthPlayCount: Long = 0,
    val totalPlayDurationMs: Long = 0,
    val weekPlayDurationMs: Long = 0,
    val topSongs: List<SongEntity> = emptyList(),
    val topArtists: List<Pair<String, Long>> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val weekAgo = now - 7L * 24 * 3600 * 1000
            val monthAgo = now - 30L * 24 * 3600 * 1000

            val totalCount = musicRepository.getTotalPlayCount().first()
            val allSongs = musicRepository.getTopPlayedSongs(200).first()
            val weekSongs = musicRepository.getSongsPlayedSince(weekAgo).first()
            val monthSongs = musicRepository.getSongsPlayedSince(monthAgo).first()

            val topSongs = allSongs.filter { it.playCount > 0 }.take(10)
            val topArtists = allSongs
                .filter { it.artist != SongEntity.UNKNOWN_ARTIST }
                .groupBy { it.artist }
                .map { (artist, songs) -> artist to songs.sumOf { it.playCount } }
                .sortedByDescending { it.second }
                .take(10)

            _uiState.value = StatisticsUiState(
                totalPlayCount = totalCount,
                weekPlayCount = weekSongs.count { it.playCount > 0 }.toLong(),
                monthPlayCount = monthSongs.count { it.playCount > 0 }.toLong(),
                totalPlayDurationMs = allSongs.sumOf { it.duration * it.playCount },
                weekPlayDurationMs = weekSongs.sumOf { it.duration },
                topSongs = topSongs,
                topArtists = topArtists,
                isLoading = false,
            )
        }
    }
}
