package com.hezi.juyumao.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hezi.juyumao.ui.components.AnimatedIconButton
import com.hezi.juyumao.ui.components.PulsingGlow
import com.hezi.juyumao.ui.components.RotatingAlbumArt
import com.hezi.juyumao.ui.lyrics.LyricsView

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) } // 0=off, 1=all, 2=one
    var showLyrics by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 100) onBack()
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Top bar
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

            Spacer(modifier = Modifier.weight(0.3f))

            // Album art or Lyrics
            if (showLyrics) {
                LyricsView(
                    lyricsData = null, // TODO: connect to actual lyrics
                    currentPositionMs = 0L,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f),
                ) {
                    PulsingGlow(
                        color = MaterialTheme.colorScheme.primary,
                        size = 300.dp,
                    )
                    RotatingAlbumArt(
                        isPlaying = isPlaying,
                        size = 240.dp,
                    )
                }
            }

            // Song info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress bar
            Column(modifier = Modifier.fillMaxWidth()) {
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
                    Text(
                        text = "0:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "0:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle
                AnimatedIconButton(onClick = { shuffleEnabled = !shuffleEnabled }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "随机",
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Previous
                AnimatedIconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Play/Pause
                AnimatedIconButton(
                    onClick = { isPlaying = !isPlaying },
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    )
                                ),
                                shape = CircleShape,
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause
                                         else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                // Next
                AnimatedIconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Repeat
                AnimatedIconButton(onClick = { repeatMode = (repeatMode + 1) % 3 }) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Default.RepeatOne
                                     else Icons.Default.Repeat,
                        contentDescription = "循环",
                        tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
