package com.hezi.juyumao.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 完整模式控制区 — 五按钮（随机/上一首/播放暂停/下一首/循环）
 */
@Composable
fun FullControlRow(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 随机播放
        IconButton(onClick = onShuffle) {
            Icon(Icons.Default.Shuffle, "随机",
                tint = if (shuffleEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp))
        }
        // 上一首
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.SkipPrevious, "上一首",
                tint = Color.White, modifier = Modifier.size(36.dp))
        }
        // 播放/暂停（大按钮）
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.Black,
                modifier = Modifier.size(32.dp),
            )
        }
        // 下一首
        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, "下一首",
                tint = Color.White, modifier = Modifier.size(36.dp))
        }
        // 循环模式
        IconButton(onClick = onRepeat) {
            Icon(
                imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "循环",
                tint = if (repeatMode > 0) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * 沉浸模式控制区 — 三按钮（上一首/播放暂停/下一首）
 */
@Composable
fun ImmersiveControlRow(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.SkipPrevious, "上一首",
                tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black, modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.SkipNext, "下一首",
                tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
        }
    }
}
