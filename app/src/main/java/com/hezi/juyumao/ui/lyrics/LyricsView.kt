package com.hezi.juyumao.ui.lyrics

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hezi.juyumao.player.audio.LyricsData
import com.hezi.juyumao.player.audio.LrcParser
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyricsData: LyricsData?,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    fontSize: Float = 18f,
    fontBold: Boolean = true,
    onLineClick: ((Long) -> Unit)? = null,
) {
    if (lyricsData == null || lyricsData.lines.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无歌词", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val currentIndex = remember(currentPositionMs) {
        LrcParser.findCurrentLineIndex(lyricsData.lines, currentPositionMs)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(index = maxOf(0, currentIndex - 3), scrollOffset = 0)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lyricsData.lines) { index, line ->
            val isCurrent = index == currentIndex
            val distance = kotlin.math.abs(index - currentIndex)

            val alpha by animateFloatAsState(
                targetValue = when {
                    isCurrent -> 1f
                    distance <= 2 -> 0.6f
                    else -> 0.3f
                },
                animationSpec = tween(300),
                label = "lyric_alpha",
            )

            val currentFontSize by animateFloatAsState(
                targetValue = if (isCurrent) fontSize + 2f else fontSize,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "lyric_font_size",
            )

            Text(
                text = line.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .padding(horizontal = 24.dp, vertical = if (isCurrent) 8.dp else 4.dp),
                fontSize = currentFontSize.sp,
                fontWeight = if (isCurrent && fontBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
