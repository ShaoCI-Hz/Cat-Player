package com.hezi.juyumao.ui.player.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 自定义进度条 — Salt Player 风格
 * 细线条 + 圆形滑块 + 拖拽时放大
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val durationFloat = duration.toFloat().coerceAtLeast(1f)
    val sliderValue = if (isDragging) dragValue
        else position.toFloat().coerceIn(0f, durationFloat)

    // 拖拽时滑块放大
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 10.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "thumb",
    )

    Column(modifier = modifier) {
        Slider(
            value = sliderValue,
            onValueChange = { isDragging = true; dragValue = it },
            onValueChangeFinished = { isDragging = false; onSeek(dragValue.toLong()) },
            valueRange = 0f..durationFloat,
            modifier = Modifier.fillMaxWidth().height(32.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                )
            },
            track = { sliderState ->
                Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                    // 背景轨道
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.5.dp))
                    )
                    // 已播放轨道
                    val fraction = (sliderState.value / sliderState.valueRange.endInclusive).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = fraction)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(1.5.dp))
                    )
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
        )

        // 时间文字
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatDuration(if (isDragging) dragValue.toLong() else position),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
