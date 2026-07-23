package com.example.smbplayer.data.audio

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crossfade manager for smooth transitions between tracks.
 * Gradually fades out the current track and fades in the next track.
 */
@Singleton
class CrossfadeManager @Inject constructor() {

    private var exoPlayer: ExoPlayer? = null
    private var crossfadeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Crossfade duration in milliseconds (0 = disabled)
    var crossfadeDurationMs: Long = 0L

    // Whether crossfade is currently active
    private val _isActive = MutableStateFlow(false)
    val isActive: Boolean get() = _isActive.value

    fun attach(player: ExoPlayer) {
        exoPlayer = player
    }

    /**
     * Start crossfade effect when approaching end of track.
     * Called periodically from PlayerRepository's position update.
     */
    fun onPositionUpdate(currentPositionMs: Long, durationMs: Long) {
        if (crossfadeDurationMs <= 0 || durationMs <= 0) return

        val remainingMs = durationMs - currentPositionMs
        if (remainingMs in 1..crossfadeDurationMs && !isActive) {
            startCrossfade()
        }
    }

    private fun startCrossfade() {
        val player = exoPlayer ?: return
        _isActive.value = true

        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val steps = 20
            val stepDelay = crossfadeDurationMs / steps

            for (i in 0..steps) {
                val progress = i.toFloat() / steps
                // Fade out: volume decreases from 1.0 to 0.0
                player.volume = (1f - progress).coerceIn(0f, 1f)
                delay(stepDelay)
            }

            // After fade out, seek to next track
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.volume = 0f

                // Fade in new track
                for (i in 0..steps) {
                    val progress = i.toFloat() / steps
                    player.volume = progress.coerceIn(0f, 1f)
                    delay(stepDelay)
                }
            }

            player.volume = 1f
            _isActive.value = false
        }
    }

    /**
     * Cancel any ongoing crossfade and reset volume.
     */
    fun cancel() {
        crossfadeJob?.cancel()
        _isActive.value = false
        exoPlayer?.volume = 1f
    }

    fun release() {
        cancel()
        scope.cancel()
        exoPlayer = null
    }
}
