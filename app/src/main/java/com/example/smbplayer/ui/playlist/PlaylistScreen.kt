package com.example.smbplayer.ui.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.smbplayer.ui.player.PlayerViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val playlist by playerViewModel.playlist.collectAsState()
    val currentIndex by playerViewModel.currentIndex.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播放列表 (${playlist.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            if (playlist.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (playlist.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "播放列表为空\n浏览文件并添加歌曲",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(playlist, key = { index, t -> "${index}_${t.source}_${t.smbPath}_${t.localUri}" }) { index, track ->
                    val isCurrent = index == currentIndex
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                playerViewModel.removeFromPlaylist(index)
                            }
                            false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(Modifier.fillMaxSize().background(Color(0xFFFF4444)), Alignment.CenterEnd) {
                                Icon(Icons.Filled.Delete, "删除", Modifier.padding(end = 20.dp), tint = Color.White)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    ) {
                        Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .clickable { playerViewModel.playTrack(track, playlist, index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrent) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column {
                            IconButton(
                                onClick = { if (index > 0) playerViewModel.moveInPlaylist(index, index - 1) },
                                modifier = Modifier.size(40.dp),
                                enabled = index > 0
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, "上移", Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { if (index < playlist.size - 1) playerViewModel.moveInPlaylist(index, index + 1) },
                                modifier = Modifier.size(40.dp),
                                enabled = index < playlist.size - 1
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, "下移", Modifier.size(18.dp))
                            }
                        }
                    }
                }
                }
            }
        }
        if (showClearDialog) { AlertDialog(onDismissRequest = { showClearDialog = false }, title = { Text("清空播放队列") }, text = { Text("确定要清空当前播放队列吗？") }, confirmButton = { TextButton(onClick = { playerViewModel.clearPlaylist(); showClearDialog = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }) }
    }
}
