package com.example.smbplayer.ui.library

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import com.example.smbplayer.data.local.AlbumEntry
import com.example.smbplayer.data.local.LocalTrack
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import com.example.smbplayer.data.smb.SmbFileEntry
import com.example.smbplayer.ui.connect.ConnectViewModel
import com.example.smbplayer.ui.favorites.FavoritesViewModel
import com.example.smbplayer.ui.player.PlayerViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel, connectViewModel: ConnectViewModel,
    playerViewModel: PlayerViewModel, favoritesViewModel: FavoritesViewModel,
    showTab: String = "songs", modifier: Modifier = Modifier
) {
    val localTracks by viewModel.localTracks.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val smbEntries by viewModel.smbEntries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val isSMBConnected by connectViewModel.isConnected.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g -> hasPermission = g }
    LaunchedEffect(hasPermission) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO) else viewModel.loadLocalTracks()
    }
    var searchQuery by remember { mutableStateOf("") }
    var browseTab by remember { mutableIntStateOf(0) }
    var selectedAlbum by remember { mutableStateOf<AlbumEntry?>(null) }
    val filteredTracks = remember(localTracks, searchQuery) { localTracks.filter { searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) } }
    val filteredAlbums = remember(albums, searchQuery) { albums.filter { searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) } }
    val filteredSmb = smbEntries.filter { searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) }
    val playHistory by playerViewModel.playHistory.collectAsState()

    Column(modifier) {
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("搜索歌曲、专辑...", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(44.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
            ),
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
        when (showTab) {
            "songs" -> SongList(filteredTracks, isLoading, hasPermission, permLauncher, viewModel, playerViewModel, playHistory, Modifier.weight(1f))
            "albums" -> if (selectedAlbum != null) AlbumDetailScreen(selectedAlbum!!, playerViewModel, onBack = { selectedAlbum = null }, Modifier.weight(1f)) else AlbumGrid(filteredAlbums, isLoading, playerViewModel, Modifier.weight(1f), { selectedAlbum = it })
            "smb" -> SmbBrowser(filteredSmb, breadcrumbs, isSMBConnected, isLoading, viewModel, playerViewModel, Modifier.weight(1f))
            "folders" -> FolderBrowser(filteredTracks, isLoading, hasPermission, playerViewModel, Modifier.weight(1f))
        }
    }
}

private fun timeGreeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "早上好"
    in 12..17 -> "下午好"
    in 18..22 -> "晚上好"
    else -> "夜深了"
}
private fun fmtDur(ms: Long) = if (ms <= 0) "" else { val s = ms / 1000; "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }
private fun fmtSize(b: Long) = when { b < 1024 -> "${b} B"; b < 1048576 -> "${b / 1024} KB"; else -> "%.1f MB".format(b / 1048576.0) }

enum class SortMode(val label: String) {
    TITLE("按标题"), ARTIST("按歌手"), ALBUM("按专辑"), DURATION("按时长"), DATE("按添加时间")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongList(
    tracks: List<LocalTrack>, isLoading: Boolean, hasPermission: Boolean,
    permLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    viewModel: LibraryViewModel, playerViewModel: PlayerViewModel,
    history: List<TrackInfo>, modifier: Modifier
) {
    if (isLoading) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)); Text("加载中...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; return }
    if (!hasPermission) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.FolderOpen, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)); Spacer(Modifier.height(12.dp)); Text("需要媒体权限", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; return }
    if (tracks.isEmpty()) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.MusicOff, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)); Spacer(Modifier.height(12.dp)); Text("没有本地音乐", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; return }

    var sortMode by remember { mutableStateOf(SortMode.TITLE) }
    var sortAscending by remember { mutableStateOf(true) }

    val sortedTracks = remember(tracks, sortMode, sortAscending) {
        val sorted = when (sortMode) {
            SortMode.TITLE -> tracks.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
            SortMode.ALBUM -> tracks.sortedBy { it.album.lowercase() }
            SortMode.DURATION -> tracks.sortedBy { it.durationMs }
            SortMode.DATE -> tracks.sortedByDescending { it.id } // Higher ID = more recent
        }
        if (sortAscending) sorted else sorted.reversed()
    }

    LazyColumn(modifier) {
        // === 问候 + 时间 ===
        item {
            Text(timeGreeting(), style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        }
        item {
            val now = java.util.Calendar.getInstance()
            val h = now.get(java.util.Calendar.HOUR_OF_DAY)
            val m = now.get(java.util.Calendar.MINUTE)
            val d = now.get(java.util.Calendar.DAY_OF_WEEK) - 1; val days = listOf("周日","周一","周二","周三","周四","周五","周六")
            Text(String.format("%02d:%02d | %s", h, m, days[if (d in 0..6) d else 0]),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }

        // === 统计卡片 ===
        item {
            val totalSongs = tracks.size
            val totalAlbums = viewModel.totalAlbumCount
            val totalArtists = viewModel.totalArtistCount
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(Icons.Filled.MusicNote, "$totalSongs", "歌曲"),
                    Triple(Icons.Filled.Album, "$totalAlbums", "专辑"),
                    Triple(Icons.Filled.Person, "$totalArtists", "歌手"),
                    Triple(Icons.Filled.Favorite, "0", "收藏")
                ).forEach { (ic, cnt, lbl) ->
                    Card(Modifier.weight(1f).padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(ic, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            Spacer(Modifier.height(4.dp))
                            Text(cnt, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // === 每日名言 ===
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                val quotes = listOf(
                    "音乐是人类通用的语言 — 朗费罗",
                    "没有音乐，生活将是一个错误 — 尼采",
                    "语言无法到达之处，音乐开始 — 安徒生",
                    "音乐给宇宙注入了灵魂 — 柏拉图",
                    "音乐的魅力在于，它触动你时，你感受不到痛苦 — 鲍勃·马利"
                )
                Text(quotes[java.util.Random().nextInt(quotes.size)], Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // === 最近播放 (始终显示标题) ===
        item {
            Text("最近播放", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 10.dp))
        }
        if (history.isNotEmpty()) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history.take(10), key = { it.smbPath + (it.localUri ?: "") }) { t ->
                        Card(Modifier.width(140.dp).clickable {
                            playerViewModel.playTrack(t, history.toList(), history.indexOf(t).coerceAtLeast(0))
                        }, shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(10.dp)) {
                                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                                    if (t.coverArtBytes != null) {
                                        AsyncImage(model = t.coverArtBytes, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Filled.MusicNote, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(t.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                                Text(t.artist, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text("暂无播放记录，播放一首歌试试吧", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        // Today''s Picks
        item {
            Text("今日推荐", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 12.dp))
        }
        if (tracks.isNotEmpty()) {
            item {
                val picks = remember { tracks.shuffled(java.util.Random(0)).take(10) }
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(picks, key = { it.id }) { track ->
                        Card(Modifier.width(130.dp).clickable {
                            playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, track.title, track.artist, track.album, track.durationMs, track.uri.toString()));
                        }, shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(10.dp)) {
                                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                                    val artUri = track.albumArtUri()
                                    if (artUri != null) AsyncImage(model = artUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Icon(Icons.Filled.MusicNote, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(track.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                                Text(track.artist, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Smart Playlists
        if (tracks.isNotEmpty()) {
            item {
                Text("智能歌单", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 12.dp))
            }
            item {
                val smartPlaylists = listOf(
                    Triple("最近添加", Icons.Filled.NewReleases, { com.example.smbplayer.domain.SmartPlaylistGenerator().recentlyAdded(tracks) }),
                    Triple("随机歌单", Icons.Filled.Shuffle, { com.example.smbplayer.domain.SmartPlaylistGenerator().randomMix(tracks) }),
                    Triple("短歌曲", Icons.Filled.Timer, { com.example.smbplayer.domain.SmartPlaylistGenerator().shortTracks(tracks) }),
                    Triple("长歌曲", Icons.Filled.AllInclusive, { com.example.smbplayer.domain.SmartPlaylistGenerator().longTracks(tracks) })
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(smartPlaylists.size) { index ->
                        val (name, icon, generator) = smartPlaylists[index]
                        Card(Modifier.width(120.dp).clickable {
                            val playlist = generator()
                            if (playlist.isNotEmpty()) playerViewModel.playTrack(playlist[0], playlist, 0)
                        }, shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }
        }

        // === 全部歌曲 (始终显示) ===
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("全部歌曲 (${sortedTracks.size}首)", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                // Sort button
                var showSortMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showSortMenu = true }, Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Sort, "排序", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(text = { Text(mode.label) }, onClick = {
                            if (sortMode == mode) sortAscending = !sortAscending
                            else { sortMode = mode; sortAscending = true }
                            showSortMenu = false
                        }, leadingIcon = {
                            if (sortMode == mode) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        })
                    }
                }
            }
        }

        // === 歌曲列表 ===
        items(sortedTracks, key = { it.id }) { track ->
            val isCurrent = playerViewModel.currentTrack.value?.localUri == track.uri.toString()
            var showMenu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().animateItem().combinedClickable(onClick = {
                playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, track.title, track.artist, track.album, track.durationMs, track.uri.toString()))
            }, onLongClick = { showMenu = true }).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = isCurrent, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    Box(Modifier.width(3.dp).height(36.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                }
                if (isCurrent) { Spacer(Modifier.width(8.dp)) }
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    val artUri = remember(track.id) { track.albumArtUri() }
                    if (artUri != null) AsyncImage(model = artUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Filled.MusicNote, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                    Text("${track.artist} · ${track.album}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(fmtDur(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("播放") }, onClick = { playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, track.title, track.artist, track.album, track.durationMs, track.uri.toString())); showMenu = false })
                DropdownMenuItem(text = { Text("添加到队列") }, onClick = { showMenu = false })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGrid(albums: List<AlbumEntry>, isLoading: Boolean, playerViewModel: PlayerViewModel, modifier: Modifier, onAlbumClick: (AlbumEntry) -> Unit = {}) {
    if (isLoading) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)); Text("加载中...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; return }
    if (albums.isEmpty()) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有专辑") }; return }
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(albums, key = { "${it.artist}_${it.name}" }) { album ->
            Card(Modifier.fillMaxWidth().animateItem().clickable {
                onAlbumClick(album)
                val list = album.tracks.map { t -> TrackInfo(TrackSource.LOCAL, t.title, t.artist, t.album, t.durationMs, t.uri.toString()) }
                album.tracks.firstOrNull()?.let { playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, it.title, it.artist, it.album, it.durationMs, it.uri.toString()), list, 0) }
            }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(10.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Album, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                    Text("${album.tracks.size}首", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SmbBrowser(entries: List<SmbFileEntry>, breadcrumbs: List<String>, isConnected: Boolean, isLoading: Boolean, viewModel: LibraryViewModel, playerViewModel: PlayerViewModel, modifier: Modifier) {
    if (isLoading) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)); Text("加载中...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; return }
    if (!isConnected) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未连接 SMB") }; return }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            breadcrumbs.forEachIndexed { i, c ->
                if (i > 0) Text(" / ", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { viewModel.navigateToBreadcrumb(i) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(if (i == 0) "根" else c, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { it.path }) { entry ->
                Row(Modifier.fillMaxWidth().animateItem().clickable {
                    if (entry.isDirectory) viewModel.navigateInto(entry)
                    else playerViewModel.playTrack(TrackInfo(TrackSource.SMB, entry.name.substringBeforeLast('.'), smbPath = entry.path, fileSize = entry.size))
                }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.MusicNote, null,
                        tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                        if (!entry.isDirectory && entry.size > 0) Text(fmtSize(entry.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderBrowser(
    tracks: List<LocalTrack>, isLoading: Boolean, hasPermission: Boolean,
    playerViewModel: PlayerViewModel, modifier: Modifier
) {
    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(48.dp))
        }
        return
    }
    if (!hasPermission) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("需要媒体权限", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Group tracks by folder path
    val folderMap = remember(tracks) {
        tracks.groupBy { track ->
            val path = track.uri.path ?: ""
            path.substringBeforeLast('/')
        }
    }
    val sortedFolders = remember(folderMap) { folderMap.keys.sorted() }

    var currentFolder by remember { mutableStateOf<String?>(null) }
    val folderTracks = currentFolder?.let { folderMap[it] } ?: emptyList()

    Column(modifier) {
        // Breadcrumb
        if (currentFolder != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { currentFolder = null }, Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", Modifier.size(20.dp))
                }
                Text(currentFolder?.substringAfterLast('/') ?: "", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Text("${folderTracks.size}首", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }

        if (currentFolder == null) {
            // Show folder list
            LazyColumn(Modifier.fillMaxSize()) {
                items(sortedFolders, key = { it }) { folder ->
                    val folderName = folder.substringAfterLast('/')
                    val trackCount = folderMap[folder]?.size ?: 0
                    Row(Modifier.fillMaxWidth().animateItem().clickable { currentFolder = folder }
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Folder, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(folderName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                            Text(folder, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$trackCount", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            // Show tracks in folder
            LazyColumn(Modifier.fillMaxSize()) {
                items(folderTracks, key = { it.id }) { track ->
                    val isCurrent = playerViewModel.currentTrack.value?.localUri == track.uri.toString()
                    Row(Modifier.fillMaxWidth().animateItem().clickable {
                        val trackInfos = folderTracks.map { t -> TrackInfo(TrackSource.LOCAL, t.title, t.artist, t.album, t.durationMs, t.uri.toString()) }
                        val idx = folderTracks.indexOf(track)
                        playerViewModel.playTrack(TrackInfo(TrackSource.LOCAL, track.title, track.artist, track.album, track.durationMs, track.uri.toString()), trackInfos, idx)
                    }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrent) {
                            Box(Modifier.width(3.dp).height(36.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                        }
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            val artUri = remember(track.id) { track.albumArtUri() }
                            if (artUri != null) AsyncImage(model = artUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Filled.MusicNote, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                            Text("${track.artist} · ${track.album}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(fmtDur(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
