package com.hezi.juyumao.ui.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hezi.juyumao.player.audio.LyricsData
import com.hezi.juyumao.player.audio.LrcParser
import kotlinx.coroutines.delay
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
        return
    }

    val currentIndex by remember(lyricsData) {
        derivedStateOf { LrcParser.findCurrentLineIndex(lyricsData.lines, currentPositionMs) }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 用户手动滚动时暂停自动滚动，5秒后恢复
    var autoScrollEnabled by remember { mutableStateOf(true) }

    // 监听用户滚动
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            autoScrollEnabled = false
        } else {
            // 用户停止滚动 5 秒后恢复自动滚动
            delay(5000)
            autoScrollEnabled = true
        }
    }

    // 自动滚动到当前行（仅在用户未手动滚动时）
    LaunchedEffect(currentIndex, autoScrollEnabled) {
        if (currentIndex >= 0 && autoScrollEnabled) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = maxOf(0, currentIndex - 4),
                    scrollOffset = -200,
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 200.dp, bottom = 300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lyricsData.lines) { index, line ->
            val isCurrent = index == currentIndex
            val distance = kotlin.math.abs(index - currentIndex)
            val inAnimRange = distance <= 5

            if (inAnimRange) {
                val alpha by animateFloatAsState(
                    targetValue = when {
                        isCurrent -> 1f
                        distance == 1 -> 0.7f
                        distance == 2 -> 0.45f
                        distance == 3 -> 0.25f
                        else -> 0.12f
                    },
                    animationSpec = tween(400),
                    label = "lyric_alpha",
                )

                val scale by animateFloatAsState(
                    targetValue = if (isCurrent) 1.08f else 0.95f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
                    label = "lyric_scale",
                )

                val offsetY by animateDpAsState(
                    targetValue = if (isCurrent) (-2).dp else 0.dp,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "lyric_offset",
                )

                val animatedFontSize by animateFloatAsState(
                    targetValue = if (isCurrent) fontSize + 4f else fontSize - 2f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f),
                    label = "lyric_font_size",
                )

                val color by animateColorAsState(
                    targetValue = if (isCurrent) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    animationSpec = tween(400),
                    label = "lyric_color",
                )

                Text(
                    text = line.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = alpha
                            this.scaleX = scale
                            this.scaleY = scale
                            this.translationY = offsetY.toPx()
                        }
                        .clickable(enabled = onLineClick != null) {
                            onLineClick?.invoke(line.timeMs)
                        }
                        .padding(horizontal = 28.dp, vertical = if (isCurrent) 12.dp else 6.dp),
                    fontSize = animatedFontSize.sp,
                    fontWeight = if (isCurrent && fontBold) FontWeight.Bold
                                else if (distance <= 1) FontWeight.Medium
                                else FontWeight.Normal,
                    color = color,
                    textAlign = TextAlign.Center,
                    lineHeight = (animatedFontSize * 1.5f).sp,
                )
            } else {
                // 动画范围外的行，静态渲染，节省性能
                Text(
                    text = line.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.12f)
                        .clickable(enabled = onLineClick != null) {
                            onLineClick?.invoke(line.timeMs)
                        }
                        .padding(horizontal = 28.dp, vertical = 6.dp),
                    fontSize = (fontSize - 2f).sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                    textAlign = TextAlign.Center,
                    lineHeight = ((fontSize - 2f) * 1.5f).sp,
                )
            }
        }
    }
}
