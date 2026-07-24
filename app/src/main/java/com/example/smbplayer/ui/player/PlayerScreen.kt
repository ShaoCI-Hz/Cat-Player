package com.example.smbplayer.ui.player

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smbplayer.data.player.PlayMode
import com.example.smbplayer.data.player.PlayerState
import com.example.smbplayer.ui.favorites.FavoritesViewModel
import com.example.smbplayer.ui.theme.CatPlayerBlack
import com.example.smbplayer.ui.theme.CatPlayerHiResGold
import com.example.smbplayer.ui.theme.PaletteUtil

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    favoritesViewModel: FavoritesViewModel,
    onBack: () -> Unit,
    onOpenPlaylist: () -> Unit = {},
) {
    val playerState by viewModel.playerState.collectAsState()
    val playMode by viewModel.playMode.collectAsState()
    val track by viewModel.currentTrack.collectAsState()
    val coverBytes by viewModel.coverArt.collectAsState()
    val audioFormatInfo by viewModel.audioFormatInfo.collectAsState()
    val isPlaying = playerState is PlayerState.Playing
    val favoritePaths by favoritesViewModel.favoritePaths.collectAsState()
    val trackPath = (track?.smbPath ?: track?.localUri).orEmpty()
    var showLyrics by remember { mutableStateOf(false) }
    var showAudioInfo by remember { mutableStateOf(false) }
    var showSceneMode by remember { mutableStateOf(false) }
    var triggerShareCard by remember { mutableStateOf(false) }
    var triggerLyricsPoster by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val isFavorite = trackPath.isNotEmpty() && trackPath in favoritePaths
    val haptic = LocalHapticFeedback.current

    // Lyrics full screen
    if (showLyrics) {
        val coverColors = remember(coverBytes) { PaletteUtil.extractColors(coverBytes) }
        LyricFullScreen(
            lyrics = viewModel.lyrics.collectAsState().value,
            currentPositionMs = viewModel.currentPosition.value,
            isPlaying = isPlaying,
            coverColors = coverColors,
            onTogglePlay = { viewModel.togglePlay() },
            onNext = { viewModel.next() },
            onPrevious = { viewModel.prev() },
            onDismiss = { showLyrics = false }
        )
        return
    }

    // Background color from cover
    val bgColor = remember(coverBytes) { PaletteUtil.extractDarkMuted(coverBytes, CatPlayerBlack) }

    // Cover rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "coverRotate")
    val coverRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "coverRotation"
    )
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "coverScale"
    )
    val playBtnScale by animateFloatAsState(
        if (isPlaying) 0.93f else 1f,
        spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "playBtn"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgColor.copy(alpha = 0.8f), CatPlayerBlack)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(28.dp), tint = Color.White.copy(alpha = 0.8f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("正在播放", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
                Row {
                    if (viewModel.lyrics.collectAsState().value.isNotEmpty()) {
                        IconButton(onClick = { showLyrics = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.LibraryMusic, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    IconButton(onClick = { showSceneMode = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Tune, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))

            // Cover art - circular for smooth rotation
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                // Color glow behind cover
                if (coverBytes != null) {
                    Box(Modifier.size(240.dp).clip(CircleShape).background(
                        Brush.radialGradient(listOf(bgColor.copy(alpha = 0.6f), Color.Transparent))
                    ))
                }
                // Circular cover - rotation looks natural
                Box(
                    Modifier.size(240.dp)
                        .graphicsLayer {
                            shadowElevation = 20f
                            shape = CircleShape
                            clip = true
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .rotate(if (isPlaying) coverRotation else 0f)
                        .scale(coverScale)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val absX = kotlin.math.abs(dragAmount.x)
                                val absY = kotlin.math.abs(dragAmount.y)
                                if (absX > absY) {
                                    if (dragAmount.x > 30) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.prev() }
                                    else if (dragAmount.x < -30) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.next() }
                                } else {
                                    val vol = (viewModel.volume.value - dragAmount.y / 400f).coerceIn(0f, 1f)
                                    viewModel.setVolume(vol)
                                }
                            }
                        },
                    Alignment.Center
                ) {
                    if (coverBytes != null) AsyncImage(model = coverBytes, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    else Icon(Icons.Filled.MusicNote, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Title
            AnimatedContent(targetState = track?.title ?: "", transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
            }, label = "title") { title ->
                Text(
                    title.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Hi-Res badge
            if (audioFormatInfo.isHiRes) {
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(4.dp), color = CatPlayerHiResGold) {
                    Text("Hi-Res", Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Artist
            Text(
                track?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // Favorite button
            if (trackPath.isNotEmpty()) {
                val heartScale by animateFloatAsState(if (isFavorite) 1.3f else 1f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "heart")
                IconButton(onClick = { favoritesViewModel.toggleFavorite(trackPath) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        null, Modifier.size(24.dp).scale(heartScale),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress bar
            PlayerProgress(viewModel)

            Spacer(Modifier.height(24.dp))

            // Main controls
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                // Previous
                IconButton(
                    onClick = { viewModel.prev() },
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Filled.SkipPrevious, null, Modifier.size(32.dp), tint = Color.White) }

                // Play/Pause
                IconButton(
                    onClick = { viewModel.togglePlay() },
                    modifier = Modifier.scale(playBtnScale).size(72.dp)
                        .graphicsLayer { shadowElevation = 16f; shape = CircleShape; clip = true }
                        .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))))
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // Next
                IconButton(
                    onClick = { viewModel.next() },
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Filled.SkipNext, null, Modifier.size(32.dp), tint = Color.White) }
            }

            Spacer(Modifier.height(16.dp))

            // Secondary controls
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                // Shuffle
                IconButton(
                    onClick = { viewModel.setPlayMode(if (playMode == PlayMode.Random) PlayMode.Sequential else PlayMode.Random) },
                    modifier = Modifier.size(44.dp)
                ) { Icon(Icons.Filled.Shuffle, null, Modifier.size(20.dp), tint = if (playMode == PlayMode.Random) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)) }

                // Share
                var showShareMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showShareMenu = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Share, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.5f))
                    }
                    DropdownMenu(expanded = showShareMenu, onDismissRequest = { showShareMenu = false }) {
                        DropdownMenuItem(text = { Text("分享文字") }, onClick = {
                            track?.let { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${it.title} - ${it.artist}") }, "Share")) }
                            showShareMenu = false
                        })
                        DropdownMenuItem(text = { Text("生成分享卡片") }, onClick = { showShareMenu = false; triggerShareCard = true })
                    }
                }

                // Playlist
                IconButton(onClick = { onOpenPlaylist() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.5f))
                }

                // Repeat
                IconButton(
                    onClick = { val modes = PlayMode.entries.filter { it != PlayMode.Random }; val i = modes.indexOf(playMode); viewModel.setPlayMode(if (i >= 0) modes[(i + 1) % modes.size] else PlayMode.Sequential) },
                    modifier = Modifier.size(44.dp)
                ) { Icon(if (playMode == PlayMode.Single) Icons.Filled.RepeatOne else Icons.Filled.Repeat, null, Modifier.size(20.dp), tint = if (playMode != PlayMode.Sequential) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)) }
            }

            Spacer(Modifier.height(16.dp))

            // Visualizer
            if (isPlaying) {
                AnimatedVisualizer(isPlaying = true, barCount = 24, barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
            }

            // Inline lyrics (max 3 lines)
            val lyrics by viewModel.lyrics.collectAsState()
            if (lyrics.isNotEmpty()) {
                LyricView(lyrics = lyrics, currentPositionMs = viewModel.currentPosition.value)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showAudioInfo) AudioInfoSheet(formatInfo = audioFormatInfo, onDismiss = { showAudioInfo = false })
    if (showSceneMode) SceneModeSheet(onDismiss = { showSceneMode = false })

    // Share card generation
    LaunchedEffect(triggerShareCard) {
        if (triggerShareCard) {
            try {
                val renderer = com.example.smbplayer.share.ShareCardRenderer(ctx)
                val coverBmp = coverBytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
                val path = renderer.generateShareCard(track?.title ?: "", track?.artist ?: "", track?.album ?: "", coverBmp)
                val file = java.io.File(path)
                val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "分享卡片"))
            } catch (e: Exception) { android.util.Log.e("PlayerScreen", "Share card failed", e) }
            triggerShareCard = false
        }
    }
}

@Composable
private fun PlayerProgress(viewModel: PlayerViewModel) {
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragTarget by remember { mutableFloatStateOf(0f) }
    val displayPos = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val animatedPos by animateFloatAsState(
        targetValue = if (isDragging) dragTarget else displayPos,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "progressAnim"
    )

    Column(Modifier.fillMaxWidth()) {
        // Custom progress bar with large touch target
        Box(
            Modifier.fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> isDragging = true; dragTarget = (offset.x / size.width).coerceIn(0f, 1f) },
                        onDrag = { change, _ -> change.consume(); dragTarget = (change.position.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { isDragging = false; viewModel.seekTo((dragTarget * duration).toLong()) },
                        onDragCancel = { isDragging = false }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Track background
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(Color.White.copy(alpha = 0.15f)))
            // Progress fill
            Box(Modifier.fillMaxWidth(animatedPos).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(
                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White))
            ))
            // Thumb indicator
            Box(Modifier.offset(x = (animatedPos * 100).dp).size(12.dp).clip(CircleShape).background(Color.White))
        }

        // Time labels
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms / 1000; return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }
