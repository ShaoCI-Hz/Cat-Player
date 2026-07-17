package com.example.smbplayer.ui.player

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val isPlaying = playerState is PlayerState.Playing
    val favoritePaths by favoritesViewModel.favoritePaths.collectAsState()
    val trackPath = (track?.smbPath ?: track?.localUri).orEmpty()
    val isFavorite = trackPath.isNotEmpty() && trackPath in favoritePaths
    val bgColor = remember(coverBytes) { PaletteUtil.extractDarkMuted(coverBytes, Color(0xFF0A0A0A)) }
    val playBtnScale by animateFloatAsState(if (isPlaying) 0.93f else 1f, spring(dampingRatio = 0.4f, stiffness = 600f), label = "playBtn")

    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = {}, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onBackground) }
            }, actions = {
                val ctx = LocalContext.current; val t = track
                IconButton(onClick = { onOpenPlaylist() }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground) }
                if (t != null) IconButton(onClick = {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}") }, "Share"))
                }) { Icon(Icons.Filled.Share, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(bgColor.copy(alpha = 0.6f), Color(0xFF0A0A0A)))).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(280.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
                if (coverBytes != null) AsyncImage(model = coverBytes, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Filled.MusicNote, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            AnimatedContent(targetState = track?.title ?: "", transitionSpec = { slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut() }, label = "title") { title ->
                Text(title.ifEmpty { "Unknown" }, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${track?.artist ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                if (trackPath.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    val heartScale by animateFloatAsState(if (isFavorite) 1.3f else 1f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "heart")
                    IconButton(onClick = { favoritesViewModel.toggleFavorite(trackPath) }, Modifier.size(20.dp)) {
                        Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, Modifier.size(18.dp).scale(heartScale), tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PlayerProgress(viewModel)
            Spacer(Modifier.height(8.dp))
            val lyrics by viewModel.lyrics.collectAsState()
            if (lyrics.isNotEmpty()) LyricView(lyrics = lyrics, currentPositionMs = viewModel.currentPosition.value)
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
}

@Composable
private fun PlayerProgress(viewModel: PlayerViewModel) {
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragTarget by remember { mutableFloatStateOf(0f) }
    val displayPos = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    Column(Modifier.fillMaxWidth()) {
        Slider(value = if (isDragging) dragTarget else displayPos, onValueChange = { isDragging = true; dragTarget = it }, onValueChangeFinished = { isDragging = false; viewModel.seekTo((dragTarget * duration).toLong()) }, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms / 1000; return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }
