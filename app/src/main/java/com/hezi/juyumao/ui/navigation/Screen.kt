package com.hezi.juyumao.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Browse : Screen("browse")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Player : Screen("player/{songId}") {
        fun createRoute(songId: Long) = "player/$songId"
    }
    data object Queue : Screen("queue")
    data object SmbConnect : Screen("smb_connect?guide={guide}") {
        fun createRoute(guide: Boolean = false) = "smb_connect?guide=$guide"
    }
    data object Equalizer : Screen("equalizer")
    data object Cache : Screen("cache")
    data object Playlist : Screen("playlist")
    data object Statistics : Screen("statistics")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Browse, "浏览", Icons.Filled.Folder, Icons.Outlined.Folder),
    BottomNavItem(Screen.Search, "搜索", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.Settings, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
)
