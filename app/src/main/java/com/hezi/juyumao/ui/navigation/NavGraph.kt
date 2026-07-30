package com.hezi.juyumao.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hezi.juyumao.ui.equalizer.EqualizerScreen
import com.hezi.juyumao.ui.home.HomeScreen
import com.hezi.juyumao.ui.player.PlayerScreen
import com.hezi.juyumao.ui.queue.QueueScreen
import com.hezi.juyumao.ui.search.SearchScreen
import com.hezi.juyumao.ui.settings.SettingsScreen
import com.hezi.juyumao.ui.smb.SmbConnectScreen

@Composable
fun JuYuMaoNavGraph(
    navController: NavHostController,
    onNavigateToPlayer: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f)) +
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                initialOffsetY = { it / 20 }
            )
        },
        exitTransition = {
            fadeOut(animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f))
        },
        popEnterTransition = {
            fadeIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f))
        },
        popExitTransition = {
            fadeOut(animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f)) +
            slideOutVertically(
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                targetOffsetY = { it / 20 }
            )
        },
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToSmb = { navController.navigate(Screen.SmbConnect.route) },
                onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
            )
        }
        composable(Screen.Browse.route) {
            com.hezi.juyumao.ui.browse.BrowseScreen()
        }
        composable(Screen.Search.route) {
            SearchScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToSmb = { navController.navigate(Screen.SmbConnect.route) },
                onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
            )
        }
        composable(Screen.Player.route) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Queue.route) {
            QueueScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SmbConnect.route) {
            SmbConnectScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Equalizer.route) {
            EqualizerScreen(onBack = { navController.popBackStack() })
        }
    }
}
