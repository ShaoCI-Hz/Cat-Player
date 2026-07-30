package com.hezi.juyumao.ui.player

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hezi.juyumao.ui.components.AnimatedIconButton
import com.hezi.juyumao.ui.components.RotatingAlbumArt

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }

    // 最简单的布局：一个 Box 撑满，Column 置底放控件，中间用 Spacer 自动填充
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶栏 - 固定高度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("正在播放", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.size(48.dp))
        }

        // 专辑区 - 固定 280dp，不参与弹性布局
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            RotatingAlbumArt(isPlaying = isPlaying, size = 220.dp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 歌曲信息 - 固定
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

        // 弹性间距 - 填充剩余空间，把下方控件推到底部
        Spacer(modifier = Modifier.weight(1f))

        // 进度条
        Slider(
            value = 0f,
            onValueChange = {},
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
            AnimatedIconButton(onClick = {}) {
                Icon(Icons.Default.SkipPrevious, "上一首", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
            }
            AnimatedIconButton(onClick = { isPlaying = !isPlaying }) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(0.8f))),
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
            AnimatedIconButton(onClick = {}) {
                Icon(Icons.Default.SkipNext, "下一首", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}
