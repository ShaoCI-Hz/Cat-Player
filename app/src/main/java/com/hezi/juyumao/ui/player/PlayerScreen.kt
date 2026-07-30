package com.hezi.juyumao.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hezi.juyumao.ui.components.AnimatedIconButton
import com.hezi.juyumao.ui.components.RotatingAlbumArt
import com.hezi.juyumao.ui.lyrics.LyricsView

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var showLyrics by remember { mutableStateOf(false) }

    // 用 BoxWithConstraints 根据屏幕高度精确分配空间，避免 scroll 导致的延迟渲染
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 100) onBack()
                }
            },
    ) {
        val screenHeight = maxHeight
        val topBarHeight = 48.dp
        val albumSize = 220.dp
        val albumPadding = 24.dp
        val controlsHeight = 200.dp // 歌曲信息 + 进度条 + 控制按钮 + 底部间距

        // 计算专辑区域高度：屏幕 - 顶栏 - 控制区 - 间距
        val albumAreaHeight = screenHeight - topBarHeight - controlsHeight - 48.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶栏
            Spacer(modifier = Modifier.height(topBarHeight))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row {
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "歌词",
                            tint = if (showLyrics) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            // 专辑区域 - 固定高度，居中显示圆盘
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(albumAreaHeight.coerceAtLeast(200.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (showLyrics) {
                    LyricsView(
                        lyricsData = null,
                        currentPositionMs = 0L,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    RotatingAlbumArt(
                        isPlaying = isPlaying,
                        size = albumSize,
                    )
                }
            }

            // 歌曲信息
            Text(
                text = "未知歌曲",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "未知艺术家",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            Slider(
                value = 0f,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedIconButton(onClick = { shuffleEnabled = !shuffleEnabled }) {
                    Icon(Icons.Default.Shuffle, "随机",
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp))
                }
                AnimatedIconButton(onClick = { }) {
                    Icon(Icons.Default.SkipPrevious, "上一首",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp))
                }
                AnimatedIconButton(onClick = { isPlaying = !isPlaying }) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                ),
                                shape = CircleShape,
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                AnimatedIconButton(onClick = { }) {
                    Icon(Icons.Default.SkipNext, "下一首",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp))
                }
                AnimatedIconButton(onClick = { repeatMode = (repeatMode + 1) % 3 }) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "循环",
                        tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
