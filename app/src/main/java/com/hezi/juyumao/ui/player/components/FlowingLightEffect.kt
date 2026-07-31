package com.hezi.juyumao.ui.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 流光动效 — Salt Player 标志性背景效果
 * 两个光斑在背景上缓慢移动，模拟光线流动
 */
@Composable
fun FlowingLightEffect(
    baseColor: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flow")

    // 光斑 1：从左上到右下，8 秒往返
    val offset1 by infiniteTransition.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "light1",
    )
    // 光斑 2：从右下到左上，12 秒往返（速度不同，产生错位感）
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1.2f, targetValue = -0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "light2",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 光斑 1：圆形渐变，左上区域
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.4f),
                    baseColor.copy(alpha = 0.0f),
                ),
                center = Offset(w * offset1, h * 0.3f),
                radius = w * 0.6f,
            ),
            radius = w * 0.6f,
            center = Offset(w * offset1, h * 0.3f),
        )

        // 光斑 2：椭圆渐变，右下区域
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.3f),
                    baseColor.copy(alpha = 0.0f),
                ),
                center = Offset(w * offset2, h * 0.7f),
                radius = w * 0.5f,
            ),
            topLeft = Offset(w * offset2 - w * 0.5f, h * 0.7f - w * 0.4f),
            size = Size(w * 1.0f, w * 0.8f),
        )
    }
}
