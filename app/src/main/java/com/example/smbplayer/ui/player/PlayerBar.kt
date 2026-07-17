package com.example.smbplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smbplayer.data.player.PlayerState

@Composable
fun PlayerBar(
    viewModel: PlayerViewModel,
    onOpenPlayer: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val track by viewModel.currentTrack.collectAsState()
    val coverBytes by viewModel.coverArt.collectAsState()

    val t = track ?: return
    val isPlaying = playerState is PlayerState.Playing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onOpenPlayer() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail 48dp
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (coverBytes != null) {
                AsyncImage(model = coverBytes, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.MusicNote, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.width(12.dp))

        // Title + artist
        Column(modifier = Modifier.weight(1f)) {
            Text(t.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.artist.isNotEmpty()) {
                Text(t.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Play/Pause button
        IconButton(onClick = { viewModel.togglePlay() }, Modifier.size(40.dp)) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (isPlaying) "暂停" else "播放",
                Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Next button
        IconButton(onClick = { viewModel.next() }, Modifier.size(40.dp)) {
            Icon(Icons.Filled.SkipNext, "下一首", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
