package com.hezi.juyumao.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 呼吸光晕：scale 1.0→1.15 + alpha 周期循环（约 2s），颜色跟随封面主色。
 * 仅 active 时创建动画，暂停时静态（避免空闲耗电）。
 */
@Composable
fun PulsingGlow(
    color: Color,
    size: Dp = 280.dp,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    // 仅 active 时创建无限动画，否则用静态值
    val pulse = if (active) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glow_scale",
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glow_alpha",
        )
        scale to alpha
    } else {
        1.0f to 0.6f
    }

    val (scale, alpha) = pulse

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.4f),
                        color.copy(alpha = 0.15f),
                        color.copy(alpha = 0f),
                    ),
                    radius = this.size.minDimension / 2,
                ),
                radius = this.size.minDimension / 2,
            )
        }
    }
}
