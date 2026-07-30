package com.hezi.juyumao.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val playbackSpeed: Float = 1.0f,
)

enum class RepeatMode {
    OFF, ONE, ALL
}
