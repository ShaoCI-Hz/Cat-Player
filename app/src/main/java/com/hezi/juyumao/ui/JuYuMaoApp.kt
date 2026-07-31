package com.hezi.juyumao.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hezi.juyumao.ui.components.MiniPlayerBar
import com.hezi.juyumao.ui.components.PremiumBottomNavBar
import com.hezi.juyumao.ui.navigation.JuYuMaoNavGraph
import com.hezi.juyumao.ui.navigation.Screen
import com.hezi.juyumao.ui.navigation.bottomNavItems
import com.hezi.juyumao.ui.theme.JuYuMaoTheme

@Composable
fun JuYuMaoApp() {
    val appViewModel: AppViewModel = hiltViewModel()
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()

    JuYuMaoTheme(themeMode = themeMode) {
        JuYuMaoAppContent(appViewModel)
    }
}

@Composable
private fun JuYuMaoAppContent(
    appViewModel: AppViewModel,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Browse.route,
        Screen.Search.route,
        Screen.Settings.route,
    )

    val currentSong by appViewModel.currentSong.collectAsStateWithLifecycle()
    val artworkUri by appViewModel.artworkUri.collectAsStateWithLifecycle()
    val isPlaying by appViewModel.isPlaying.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                    initialOffsetY = { it }
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                    targetOffsetY = { it }
                ) + fadeOut(),
            ) {
                androidx.compose.foundation.layout.Column {
                    MiniPlayerBar(
                        onPlayerClick = {
                            val songId = currentSong?.id ?: return@MiniPlayerBar
                            navController.navigate(Screen.Player.createRoute(songId))
                        },
                        songTitle = currentSong?.title,
                        songArtist = currentSong?.artist,
                        artworkUri = artworkUri,
                        isPlaying = isPlaying,
                        onPlayPauseClick = { appViewModel.togglePlay() },
                    )
                    PremiumBottomNavBar(
                        items = bottomNavItems,
                        currentRoute = currentRoute ?: Screen.Home.route,
                        onItemSelected = { item ->
                            if (item.screen.route != currentRoute) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        val isPlayerPage = currentRoute?.startsWith("player/") == true
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isPlayerPage) Modifier.padding(paddingValues) else Modifier),
        ) {
            JuYuMaoNavGraph(navController = navController)
        }
    }
}
