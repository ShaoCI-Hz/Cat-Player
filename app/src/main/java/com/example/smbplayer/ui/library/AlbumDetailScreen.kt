package com.example.smbplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.smbplayer.data.local.AlbumEntry
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import com.example.smbplayer.ui.player.PlayerViewModel

@Composable
fun AlbumDetailScreen(
    album: AlbumEntry,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tracks = album.tracks
    Column(modifier = modifier) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        LazyColumn(Modifier.padding(horizontal = 12.dp)) {
        // Album header
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(220.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Album, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(14.dp))
                Text(album.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                Text("${album.artist} · ${tracks.size} 首", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Button(onClick = {
                    val list = tracks.map { t -> TrackInfo(TrackSource.LOCAL, t.title, t.artist, t.album, t.durationMs, t.uri.toString()) }
                    tracks.firstOrNull()?.let { playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, it.title, it.artist, it.album, it.durationMs, it.uri.toString()), list, 0) }
                }, modifier = Modifier.fillMaxWidth(0.7f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("全部播放")
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        
        // Track list
        items(tracks) { track ->
            Row(Modifier.fillMaxWidth().clickable {
                val list = tracks.map { t -> TrackInfo(TrackSource.LOCAL, t.title, t.artist, t.album, t.durationMs, t.uri.toString()) }
                playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, track.title, track.artist, track.album, track.durationMs, track.uri.toString()), list, tracks.indexOf(track))
            }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${tracks.indexOf(track) + 1}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(32.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(2.dp))
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}
}
