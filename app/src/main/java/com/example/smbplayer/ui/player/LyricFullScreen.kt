package com.example.smbplayer.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smbplayer.data.lyrics.LyricLine
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LyricFullScreen(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无歌词", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Animated background flow
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val density = LocalDensity.current

    // 3 flowing light beams with different speeds and angles
    val angle1 by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(animation = tween(12000, easing = LinearEasing)), label = "a1")
    val angle2 by infiniteTransition.animateFloat(120f, 480f, infiniteRepeatable(animation = tween(15000, easing = LinearEasing)), label = "a2")
    val angle3 by infiniteTransition.animateFloat(240f, 600f, infiniteRepeatable(animation = tween(18000, easing = LinearEasing)), label = "a3")
    val glowOffset by infiniteTransition.animateFloat(-800f, 800f, infiniteRepeatable(animation = tween(10000, easing = LinearEasing)), label = "glow")

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    // Lyrics state
    var lastIdx by remember { mutableIntStateOf(0) }
    val currentIndex = if (lastIdx in lyrics.indices && currentPositionMs >= lyrics[lastIdx].timestampMs) {
        while (lastIdx + 1 < lyrics.size && lyrics[lastIdx + 1].timestampMs <= currentPositionMs) lastIdx++
        lastIdx
    } else {
        lastIdx = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }
        lastIdx
    }.coerceAtLeast(0)

    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (currentIndex in 0 until lyrics.size) {
            listState.animateScrollToItem((currentIndex - 5).coerceAtLeast(0))
        }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Animated background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h / 2 + glowOffset * size.height / 1600f

            val colors = listOf(
                primary.copy(alpha = 0.08f),
                secondary.copy(alpha = 0.06f),
                tertiary.copy(alpha = 0.06f)
            )
            val angles = listOf(angle1, angle2, angle3)

            for (i in colors.indices) {
                val a = angles[i]
                val rad = a * Math.PI.toFloat() / 180f
                val dx = sin(rad) * w * 0.6f
                val dy = cos(rad) * h * 0.4f

                // Glowing beam
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, colors[i], colors[i], Color.Transparent),
                        start = Offset(cx, cy),
                        end = Offset(cx + dx, cy + dy)
                    ),
                    start = Offset(cx, cy),
                    end = Offset(cx + dx, cy + dy),
                    strokeWidth = w * 0.35f,
                    cap = StrokeCap.Round
                )
            }

            // Center soft glow
            val glowSize = w * 0.4f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(cx, cy)
                ),
                radius = glowSize,
                center = Offset(cx, cy)
            )
        }

        // Top bar with dismiss
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.KeyboardArrowDown, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.weight(1f))
            Text("歌词", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        // Lyrics list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(top = 48.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(160.dp)) }
            itemsIndexed(lyrics) { i, line ->
                val isCurrent = i == currentIndex
                Text(
                    text = line.text.ifEmpty { "..." },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    fontSize = if (isCurrent) 24.sp else 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    lineHeight = if (isCurrent) 36.sp else 26.sp
                )
            }
            item { Spacer(Modifier.height(160.dp)) }
        }

        // Bottom controls
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { Icon(Icons.Filled.SkipPrevious, "上一首", Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onTogglePlay) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "播放", Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, "下一首", Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
