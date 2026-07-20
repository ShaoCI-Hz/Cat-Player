package com.example.smbplayer.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smbplayer.data.local.ArtistEntry
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import com.example.smbplayer.ui.player.PlayerViewModel

@Composable
fun ArtistScreen(
    artists: List<ArtistEntry>,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有歌手", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = modifier.padding(horizontal = 12.dp)) {
        item { Text("歌手", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) }
        items(artists, key = { it.name }) { artist ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                val tracks = artist.tracks.map { t -> TrackInfo(TrackSource.LOCAL, t.title, t.artist, t.album, t.durationMs, t.uri.toString()) }
                artist.tracks.firstOrNull()?.let { playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, it.title, it.artist, it.album, it.durationMs, it.uri.toString()), tracks, 0) }
            }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(artist.name.first().toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(2.dp))
                        Text("${artist.tracks.size} 首歌曲", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
