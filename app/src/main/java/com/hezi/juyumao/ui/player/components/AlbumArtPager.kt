package com.hezi.juyumao.ui.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * 专辑封面展示组件
 * 支持方形圆角和圆形唱片两种样式
 */
@Composable
fun AlbumArtPager(
    artworkUri: String?,
    isPlaying: Boolean,
    isRound: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
) {
    // 旋转动画（圆形唱片模式）
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    val shape = if (isRound) CircleShape else RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (isRound && isPlaying) rotationZ = rotation
            }
            .shadow(24.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUri != null) {
            AsyncImage(
                model = File(artworkUri),
                contentDescription = "专辑封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // 占位：渐变背景 + 音符图标
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondary?.copy(alpha = 0.3f)
                                ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                    )
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MusicNote, null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}
