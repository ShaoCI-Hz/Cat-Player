package com.example.smbplayer.ui.player

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import com.example.smbplayer.ui.theme.PaletteUtil

@OptIn(ExperimentalMaterial3Api::class)
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

    if (showLyrics) {
        val coverColors = remember(coverBytes) { com.example.smbplayer.ui.theme.PaletteUtil.extractColors(coverBytes) }
        LyricFullScreen(lyrics = viewModel.lyrics.collectAsState().value, currentPositionMs = viewModel.currentPosition.value,
            isPlaying = isPlaying, coverColors = coverColors,
            onTogglePlay = { viewModel.togglePlay() }, onNext = { viewModel.next() }, onPrevious = { viewModel.prev() },
            onDismiss = { showLyrics = false })
        return
    }
    val bgColor = remember(coverBytes) { PaletteUtil.extractDarkMuted(coverBytes, Color(0xFF0A0A0A)) }
    val playBtnScale by animateFloatAsState(if (isPlaying) 0.93f else 1f, spring(dampingRatio = 0.4f, stiffness = 600f), label = "playBtn")

    // P1: Cover rotation animation - rotates when playing, stops when paused
    val infiniteTransition = rememberInfiniteTransition(label = "coverRotate")
    val coverRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coverRotation"
    )
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "coverScale"
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = {}, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onBackground) }
            }, actions = {
                val t = track
                if (viewModel.lyrics.collectAsState().value.isNotEmpty()) IconButton(onClick = { showLyrics = true }) { Icon(Icons.Filled.LibraryMusic, "歌词", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = { onOpenPlaylist() }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground) }
                // #10: Scene mode button
                IconButton(onClick = { showSceneMode = true }) { Icon(Icons.Filled.Tune, "场景模式", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground) }
                // Share button with menu
                var showShareMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showShareMenu = true }) { Icon(Icons.Filled.Share, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground) }
                    DropdownMenu(expanded = showShareMenu, onDismissRequest = { showShareMenu = false }) {
                        DropdownMenuItem(text = { Text("分享文字") }, onClick = {
                            t?.let { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${it.title} - ${it.artist}") }, "Share")) }
                            showShareMenu = false
                        })
                        DropdownMenuItem(text = { Text("生成分享卡片") }, onClick = {
                            showShareMenu = false
                            triggerShareCard = true
                        })
                        if (viewModel.lyrics.collectAsState().value.isNotEmpty()) {
                            DropdownMenuItem(text = { Text("生成歌词海报") }, onClick = {
                                showShareMenu = false
                                triggerLyricsPoster = true
                            })
                        }
                    }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(bgColor.copy(alpha = 0.6f), Color(0xFF0A0A0A)))).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            // Cover art with gesture support + rotation animation (P1 + #3 enhanced)
            var volumeAdjust by remember { mutableFloatStateOf(0f) }
            val haptic = LocalHapticFeedback.current
            Box(
                Modifier.size(280.dp).clip(RoundedCornerShape(140.dp)).border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(140.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .rotate(if (isPlaying) coverRotation else 0f)
                    .scale(coverScale)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {},
                            onDragCancel = {},
                            onHorizontalDrag = { _, dragAmount ->
                                // #3: Velocity-aware swipe with haptic feedback
                                if (dragAmount > 30) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.prev()
                                } else if (dragAmount < -30) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.next()
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            volumeAdjust = (-dragAmount / 400f).coerceIn(-0.15f, 0.15f)
                            val newVol = (viewModel.volume.value + volumeAdjust).coerceIn(0f, 1f)
                            viewModel.setVolume(newVol)
                            // Haptic feedback on volume change
                            if (kotlin.math.abs(dragAmount) > 20) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                Alignment.Center
            ) {
                if (coverBytes != null) AsyncImage(model = coverBytes, null, Modifier.fillMaxSize().clip(RoundedCornerShape(140.dp)), contentScale = ContentScale.Crop)
                else Icon(Icons.Filled.MusicNote, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            // Title with Hi-Res badge
            AnimatedContent(targetState = track?.title ?: "", transitionSpec = { slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut() }, label = "title") { title ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(title.ifEmpty { "Unknown" }, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    // #4: Hi-Res badge
                    if (audioFormatInfo.isHiRes) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFD700)) {
                            Text("Hi-Res", Modifier.padding(horizontal = 4.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Audio format info button
            if (audioFormatInfo.sampleRate > 0) {
                TextButton(onClick = { showAudioInfo = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(
                        "${audioFormatInfo.codecDisplay} · ${audioFormatInfo.sampleRateDisplay}${if (audioFormatInfo.bitDepth > 0) " · ${audioFormatInfo.bitDepthDisplay}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${track?.artist ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                if (trackPath.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    // P7: Favorite with explosion animation
                    val heartScale by animateFloatAsState(if (isFavorite) 1.3f else 1f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "heart")
                    var showExplosion by remember { mutableStateOf(false) }
                    Box(contentAlignment = Alignment.Center) {
                        // Explosion particles
                        if (showExplosion) {
                            repeat(8) { index ->
                                val angle = index * 45f
                                val rad = Math.toRadians(angle.toDouble())
                                val targetX = (Math.cos(rad) * 30).toFloat()
                                val targetY = (Math.sin(rad) * 30).toFloat()
                                val animX by animateFloatAsState(targetValue = targetX, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "explosionX$index")
                                val animY by animateFloatAsState(targetValue = targetY, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "explosionY$index")
                                val animAlpha by animateFloatAsState(targetValue = 0f, animationSpec = tween(400), label = "explosionAlpha$index")
                                Icon(
                                    Icons.Filled.Favorite, null,
                                    Modifier.size(8.dp).offset(x = animX.dp, y = animY.dp).graphicsLayer(alpha = animAlpha),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = {
                            favoritesViewModel.toggleFavorite(trackPath)
                            if (!isFavorite) {
                                showExplosion = true
                            }
                        }, Modifier.size(24.dp)) {
                            Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, Modifier.size(20.dp).scale(heartScale), tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Auto-hide explosion
                        LaunchedEffect(showExplosion) {
                            if (showExplosion) {
                                kotlinx.coroutines.delay(450)
                                showExplosion = false
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PlayerProgress(viewModel)
            Spacer(Modifier.height(8.dp))
            val lyrics by viewModel.lyrics.collectAsState()
            if (lyrics.isNotEmpty()) LyricView(lyrics = lyrics, currentPositionMs = viewModel.currentPosition.value)
            // Audio Visualizer (B6)
            if (isPlaying) {
                Spacer(Modifier.height(8.dp))
                AnimatedVisualizer(
                    isPlaying = isPlaying,
                    barCount = 24,
                    barColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setPlayMode(if (playMode == PlayMode.Random) PlayMode.Sequential else PlayMode.Random) }) { Icon(Icons.Filled.Shuffle, null, Modifier.size(20.dp), tint = if (playMode == PlayMode.Random) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { viewModel.prev() }) { Icon(Icons.Filled.SkipPrevious, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = { viewModel.togglePlay() }, Modifier.scale(playBtnScale).size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.next() }) { Icon(Icons.Filled.SkipNext, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = { val modes = PlayMode.entries.filter { it != PlayMode.Random }; val i = modes.indexOf(playMode); viewModel.setPlayMode(if (i >= 0) modes[(i + 1) % modes.size] else PlayMode.Sequential) }) {
                    Icon(if (playMode == PlayMode.Single) Icons.Filled.RepeatOne else Icons.Filled.Repeat, null, Modifier.size(20.dp), tint = if (playMode != PlayMode.Sequential) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // #4: Audio Info Sheet dialog
    if (showAudioInfo) {
        AudioInfoSheet(formatInfo = audioFormatInfo, onDismiss = { showAudioInfo = false })
    }

    // #10: Scene Mode dialog
    if (showSceneMode) {
        SceneModeSheet(onDismiss = { showSceneMode = false })
    }

    // #7: Share card generation
    LaunchedEffect(triggerShareCard) {
        if (triggerShareCard) {
            try {
                val renderer = com.example.smbplayer.share.ShareCardRenderer(ctx)
                val coverBmp = coverBytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
                val path = renderer.generateShareCard(
                    title = track?.title ?: "Unknown",
                    artist = track?.artist ?: "Unknown",
                    album = track?.album ?: "",
                    coverBitmap = coverBmp
                )
                val file = java.io.File(path)
                val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "分享卡片"))
            } catch (_: Exception) {}
            triggerShareCard = false
        }
    }

    // #8: Lyrics poster generation
    LaunchedEffect(triggerLyricsPoster) {
        if (triggerLyricsPoster) {
            try {
                val renderer = com.example.smbplayer.share.LyricsPosterRenderer(ctx)
                val coverBmp = coverBytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
                val lyricLines = viewModel.lyrics.value.take(4).map { it.text }
                val path = renderer.generateLyricsPoster(
                    lyrics = lyricLines,
                    title = track?.title ?: "Unknown",
                    artist = track?.artist ?: "Unknown",
                    coverBitmap = coverBmp
                )
                val file = java.io.File(path)
                val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "歌词海报"))
            } catch (_: Exception) {}
            triggerLyricsPoster = false
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

    // P2: Gradient progress bar
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            // Custom gradient track
            val trackHeight = 4.dp
            Box(Modifier.fillMaxWidth().height(trackHeight).align(Alignment.CenterStart)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                Box(Modifier.fillMaxWidth(animatedPos).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(
                    Brush.horizontalGradient(listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primary
                    ))
                ))
            }
        }
        Slider(
            value = if (isDragging) dragTarget else animatedPos,
            onValueChange = { isDragging = true; dragTarget = it },
            onValueChangeFinished = { isDragging = false; viewModel.seekTo((dragTarget * duration).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.Transparent
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms / 1000; return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }
