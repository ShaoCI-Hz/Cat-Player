package com.example.smbplayer.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import com.example.smbplayer.ui.player.PlayerViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val favoritePaths by viewModel.favoritePaths.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "收藏 (${favoritePaths.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (favoritePaths.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有收藏歌曲",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(favoritePaths, key = { it }) { path ->
                    val isLocal = path.startsWith("content://")
                    val source = if (isLocal) TrackSource.LOCAL else TrackSource.SMB
                    val fileName = path.substringAfterLast('/')
                    val dashIdx = fileName.indexOf(" - "); val title = if (dashIdx > 0) fileName.substring(dashIdx + 3).substringBeforeLast('.') else fileName.substringBeforeLast('.'); val guessArtist = if (dashIdx > 0) fileName.substring(0, dashIdx) else ""
                    val displayPath = if (isLocal) "本地文件" else path
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val track = TrackInfo(
                                    source = source,
                                    smbPath = if (isLocal) "" else path,
                                    localUri = if (isLocal) path else null,
                                    title = title,
                                    fileSize = 0
                                )
                                playerViewModel.playTrack(track)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = displayPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.removeFavorite(path) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "取消收藏",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
