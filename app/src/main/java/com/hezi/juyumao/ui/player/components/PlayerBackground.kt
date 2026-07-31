package com.hezi.juyumao.ui.player.components

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 播放页背景 — 三层叠加
 * 第一层：封面高斯模糊
 * 第二层：Palette 主色调渐变叠加
 * 第三层：流光动效
 */
@Composable
fun PlayerBackground(
    artworkUri: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A2E)) }

    // 异步提取封面主色调
    LaunchedEffect(artworkUri) {
        if (artworkUri != null) {
            try {
                dominantColor = withContext(Dispatchers.IO) {
                    var bitmap: android.graphics.Bitmap? = null
                    try {
                        bitmap = BitmapFactory.decodeFile(artworkUri)
                        if (bitmap != null) {
                            val palette = Palette.from(bitmap).generate()
                            val swatch = palette.dominantSwatch
                                ?: palette.vibrantSwatch
                                ?: palette.mutedSwatch
                            if (swatch != null) Color(swatch.rgb) else Color(0xFF1A1A2E)
                        } else Color(0xFF1A1A2E)
                    } catch (_: Exception) {
                        Color(0xFF1A1A2E)
                    } finally {
                        bitmap?.recycle()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // 主色调平滑过渡
    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(800),
        label = "bg_color",
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 层 1：封面高斯模糊
        if (artworkUri != null) {
            AsyncImage(
                model = File(artworkUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(30.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.7f,
            )
        }

        // 层 2：主色调渐变叠加
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.7f),
                    )
                )
            )
        )

        // 层 3：流光动效
        FlowingLightEffect(baseColor = animatedColor)
    }
}
