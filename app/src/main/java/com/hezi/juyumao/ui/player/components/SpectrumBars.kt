package com.hezi.juyumao.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 实时频谱柱状图（T11.4）
 * 数据来自 SpectrumAnalyzer（Visualizer FFT），播放页/均衡器页复用。
 * @param bars 归一化 0..1 的柱状数据
 */
@Composable
fun SpectrumBars(
    bars: FloatArray,
    color: Color,
    modifier: Modifier = Modifier,
    barWidth: Dp = 4.dp,
    gap: Dp = 3.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        val total = bars.size
        if (total == 0) return@Canvas
        val barW = barWidth.toPx()
        val gapPx = gap.toPx()
        val totalW = total * barW + (total - 1) * gapPx
        val startX = (size.width - totalW) / 2f
        val maxH = size.height

        for (i in 0 until total) {
            val value = bars[i].coerceIn(0f, 1f)
            val h = (value * maxH).coerceAtLeast(2f)
            val x = startX + i * (barW + gapPx)
            val y = maxH - h
            drawRoundRect(
                color = color.copy(alpha = 0.5f + value * 0.5f),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}
