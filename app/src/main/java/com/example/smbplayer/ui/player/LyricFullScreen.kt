package com.example.smbplayer.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smbplayer.data.lyrics.LyricLine
import com.example.smbplayer.ui.theme.CatPlayerBlack

@Composable
fun LyricFullScreen(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    coverColors: List<Color>,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无歌词", color = Color.White.copy(alpha = 0.6f))
        }
        return
    }

    // Slow background color drift - barely perceptible, like Apple Music
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offsetX by infiniteTransition.animateFloat(
        -0.2f, 0.2f,
        infiniteRepeatable(animation = tween(18000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "ox"
    )
    val offsetY by infiniteTransition.animateFloat(
        -0.15f, 0.15f,
        infiniteRepeatable(animation = tween(22000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "oy"
    )

    // Build gradient colors from cover art palette
    val bgColors = remember(coverColors) {
        val c = coverColors.take(3)
        listOf(
            c.getOrElse(0) { Color(0xFF1A1C2E) },
            c.getOrElse(1) { Color(0xFF3B1D5E) },
            c.getOrElse(2) { Color(0xFF0D1117) }
        ).map { it.copy(alpha = 0.85f) } + listOf(CatPlayerBlack.copy(alpha = 0.95f))
    }

    // Auto-invert text color based on background brightness
    val dominantColor = coverColors.firstOrNull() ?: Color(0xFF1A1C2E)
    val brightness = (dominantColor.red * 0.299f + dominantColor.green * 0.587f + dominantColor.blue * 0.114f)
    val textColor = if (brightness > 0.5f) Color.Black else Color.White
    val dimTextColor = textColor.copy(alpha = 0.4f)

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
            listState.animateScrollToItem(
                index = (currentIndex - 5).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(CatPlayerBlack)
    ) {
        // Background: album-art-derived gradient with slow drift
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cx = size.width * (0.5f + offsetX)
                    val cy = size.height * (0.5f + offsetY)
                    val radius = maxOf(size.width, size.height) * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = bgColors,
                            center = Offset(cx, cy),
                            radius = radius
                        ),
                        center = Offset(cx, cy),
                        radius = radius
                    )
                }
        )

        // Additional dimming overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CatPlayerBlack.copy(alpha = 0.3f), Color.Transparent, CatPlayerBlack.copy(alpha = 0.6f))
                    )
                )
        )

        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.KeyboardArrowDown, "关闭", tint = dimTextColor)
            }
            Text("歌词", style = MaterialTheme.typography.titleSmall, color = dimTextColor)
            Spacer(Modifier.size(48.dp))
        }

        // Lyrics
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 56.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(180.dp)) }
            itemsIndexed(lyrics, key = { i, _ -> i }) { i, line ->
                val isCurrent = i == currentIndex

                val fontSize by animateFloatAsState(
                    targetValue = if (isCurrent) 22f else 16f,
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    label = "fs$i"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isCurrent) 1f else 0.3f,
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    label = "al$i"
                )

                Text(
                    text = line.text.ifEmpty { "…" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    fontSize = fontSize.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = textColor.copy(alpha = alpha),  // Auto-inverted based on background
                    lineHeight = (if (isCurrent) 34 else 24).sp,
                )
            }
            item { Spacer(Modifier.height(180.dp)) }
        }

        // Bottom controls (minimal)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, CatPlayerBlack.copy(alpha = 0.7f), CatPlayerBlack.copy(alpha = 0.9f))
                    )
                )
                .padding(top = 20.dp, bottom = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, "上一首", Modifier.size(28.dp), tint = Color.White.copy(alpha = 0.7f))
                }
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "播放",
                        Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, "下一首", Modifier.size(28.dp), tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
