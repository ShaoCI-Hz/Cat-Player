package com.example.smbplayer.ui.player

import android.media.audiofx.Visualizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

/**
 * Real-time FFT audio spectrum visualizer.
 * Uses Android's Visualizer API to capture FFT data and render as animated bars.
 */
@Composable
fun AudioVisualizer(
    audioSessionId: Int,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    barColor: Color = MaterialTheme.colorScheme.primary,
    sensitivity: Float = 1.5f
) {
    var fftData by remember { mutableStateOf<ByteArray?>(null) }
    val visualizer = remember(audioSessionId) {
        try {
            if (audioSessionId > 0) {
                Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1] // Max capture size
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {}
                        override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
                            fftData = fft.copyOf()
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, false, true)
                    enabled = true
                }
            } else null
        } catch (_: Exception) { null }
    }

    DisposableEffect(audioSessionId) {
        onDispose {
            try { visualizer?.enabled = false; visualizer?.release() } catch (_: Exception) {}
        }
    }

    // Smooth animation for bar heights
    val animatedHeights = remember { MutableList(barCount) { Animatable(0f) } }

    LaunchedEffect(fftData) {
        val data = fftData ?: return@LaunchedEffect
        val magnitudes = calculateMagnitudes(data, barCount, sensitivity)
        magnitudes.forEachIndexed { index, magnitude ->
            if (index < animatedHeights.size) {
                animatedHeights[index].animateTo(
                    targetValue = magnitude,
                    animationSpec = tween(durationMillis = 60, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val barWidth = size.width / barCount * 0.7f
        val gap = size.width / barCount * 0.3f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + gap / 2
            val height = (animatedHeights.getOrNull(i)?.value ?: 0f) * maxHeight

            drawLine(
                color = barColor.copy(alpha = 0.6f + 0.4f * (animatedHeights.getOrNull(i)?.value ?: 0f)),
                start = Offset(x + barWidth / 2, maxHeight),
                end = Offset(x + barWidth / 2, maxHeight - height),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Calculate magnitude spectrum from FFT data for visualization.
 * Returns normalized values (0.0 to 1.0) for each bar.
 */
private fun calculateMagnitudes(fft: ByteArray, barCount: Int, sensitivity: Float): List<Float> {
    if (fft.isEmpty()) return List(barCount) { 0f }

    val magnitudes = mutableListOf<Float>()
    val binCount = fft.size / 2

    for (i in 0 until barCount) {
        // Map bar index to FFT bin range (logarithmic distribution)
        val startBin = (binCount * i / barCount).coerceAtLeast(1)
        val endBin = (binCount * (i + 1) / barCount).coerceAtMost(binCount - 1)

        var sum = 0f
        var count = 0
        for (bin in startBin until endBin) {
            val real = fft[bin * 2].toFloat()
            val imag = fft[bin * 2 + 1].toFloat()
            val magnitude = Math.sqrt((real * real + imag * imag).toDouble()).toFloat()
            sum += magnitude
            count++
        }

        val avgMagnitude = if (count > 0) sum / count else 0f
        // Normalize to 0-1 range with sensitivity adjustment
        val normalized = (avgMagnitude / 128f * sensitivity).coerceIn(0f, 1f)
        magnitudes.add(normalized)
    }

    return magnitudes
}

/**
 * Simple spectrum visualizer that uses animation without real FFT data.
 * Used as fallback when Visualizer API is not available.
 */
@Composable
fun AnimatedVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")

    val barValues = List(barCount) { index ->
        val phase = index * 0.2f
        val speed = 800 + (index % 3) * 200
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isPlaying) 1f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(speed, easing = LinearEasing, delayMillis = (phase * 100).toInt()),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val barWidth = size.width / barCount * 0.7f
        val gap = size.width / barCount * 0.3f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + gap / 2
            val animatedValue = barValues[i].value
            // Create a wave-like pattern
            val heightFactor = (0.3f + 0.7f * animatedValue) * (0.5f + 0.5f * Math.sin(i * 0.5).toFloat())
            val height = heightFactor * maxHeight * 0.8f

            drawLine(
                color = barColor.copy(alpha = 0.4f + 0.6f * heightFactor),
                start = Offset(x + barWidth / 2, maxHeight),
                end = Offset(x + barWidth / 2, maxHeight - height),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
