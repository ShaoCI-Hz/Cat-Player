package com.hezi.juyumao.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun RotatingAlbumArt(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    artworkUri: String? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "album_rotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "album_rotation",
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = if (isPlaying) rotation else 0f
            }
            .shadow(elevation = 20.dp, shape = CircleShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUri != null) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(artworkUri))
                    .crossfade(true)
                    .build(),
                contentDescription = "专辑封面",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "专辑封面",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.4f),
            )
        }
    }
}
