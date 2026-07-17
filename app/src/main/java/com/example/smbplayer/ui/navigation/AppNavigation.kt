package com.example.smbplayer.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smbplayer.data.player.PlayerState
import com.example.smbplayer.ui.connect.ConnectViewModel
import com.example.smbplayer.ui.favorites.FavoritesScreen
import com.example.smbplayer.ui.favorites.FavoritesViewModel
import com.example.smbplayer.ui.library.LibraryScreen
import com.example.smbplayer.ui.library.LibraryViewModel
import com.example.smbplayer.ui.player.PlayerBar
import com.example.smbplayer.ui.player.PlayerScreen
import com.example.smbplayer.ui.player.PlayerViewModel
import com.example.smbplayer.ui.playlist.PlaylistScreen
import com.example.smbplayer.ui.settings.SettingsScreen
import com.example.smbplayer.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbPlayerAppContent() {
    val connectViewModel: ConnectViewModel = hiltViewModel()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val playerState by playerViewModel.playerState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showPlayerScreen by remember { mutableStateOf(false) }
    var showSMBConnect by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    var browseSubTab by remember { mutableIntStateOf(0) } // 0=albums, 1=SMB

    if (showPlayerScreen && playerState !is PlayerState.Idle && playerState !is PlayerState.Error) {
        PlayerScreen(
            viewModel = playerViewModel,
            favoritesViewModel = favoritesViewModel,
            onBack = { showPlayerScreen = false },
            onOpenPlaylist = { showPlaylist = true; showPlayerScreen = false }
        )
        return
    }

    if (showPlaylist) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("播放队列") },
                    navigationIcon = {
                        IconButton(onClick = { showPlaylist = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    }
                )
            }
        ) { padding ->
            PlaylistScreen(playerViewModel = playerViewModel, modifier = Modifier.padding(padding).fillMaxSize())
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cat Player", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { showSMBConnect = true }) { Icon(Icons.Filled.Cloud, "SMB连接") }
                }
            )
        },
        bottomBar = {
            Column {
                if (playerState !is PlayerState.Idle && playerState !is PlayerState.Error) {
                    PlayerBar(viewModel = playerViewModel, onOpenPlayer = { showPlayerScreen = true })
                }
                FloatingNavigationBar(
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    tabs = listOf(NavTab("首页", Icons.Filled.Home), NavTab("浏览", Icons.Filled.Search), NavTab("资料库", Icons.Filled.LibraryMusic))
                )
            }
        }
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues)
        Crossfade(targetState = selectedTab, label = "tabCrossfade") { tab ->
            when (tab) {
                0 -> LibraryScreen(libraryViewModel, connectViewModel, playerViewModel, favoritesViewModel, "songs", modifier)
                1 -> {
                    Column(modifier) {
                        // Sub-tabs: evenly split
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(onClick = { browseSubTab = 0 }, modifier = Modifier.weight(1f)) {
                                Text("专辑", color = if (browseSubTab == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (browseSubTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                            TextButton(onClick = { browseSubTab = 1 }, modifier = Modifier.weight(1f)) {
                                Text("SMB", color = if (browseSubTab == 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (browseSubTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        when (browseSubTab) {
                            0 -> LibraryScreen(libraryViewModel, connectViewModel, playerViewModel, favoritesViewModel, "albums", Modifier.weight(1f))
                            1 -> LibraryScreen(libraryViewModel, connectViewModel, playerViewModel, favoritesViewModel, "smb", Modifier.weight(1f))
                        }
                    }
                }
                2 -> {
                    Column(modifier) {
                        // Library items
                        FavoritesScreen(viewModel = favoritesViewModel, playerViewModel = playerViewModel, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showSettings = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Settings, null, Modifier.size(20.dp))
                            Text("  设置", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showSMBConnect) SMBConnectDialog(connectViewModel, onDismiss = { showSMBConnect = false })
    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), dragHandle = {}) {
            SettingsScreen(viewModel = settingsViewModel, modifier = Modifier.padding(bottom = 32.dp))
        }
    }
}
