package com.example.smbplayer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Pre-defined color themes for the app.
 */
enum class AppThemePreset(
    val displayName: String,
    val primary: Color,
    val background: Color,
    val surface: Color
) {
    SPOTIFY_GREEN("Spotify 绿", Color(0xFF1ED760), Color(0xFF0A0A0A), Color(0xFF121212)),
    APPLE_RED("Apple 红", Color(0xFFFF3B30), Color(0xFF000000), Color(0xFF1C1C1E)),
    TIDAL_BLUE("Tidal 蓝", Color(0xFF00FFFF), Color(0xFF000000), Color(0xFF121212)),
    YOUTUBE_RED("YouTube 红", Color(0xFFFF0000), Color(0xFF0F0F0F), Color(0xFF272727)),
    DEEP_PURPLE("深紫", Color(0xFFBB86FC), Color(0xFF0A0A0A), Color(0xFF121212)),
    OCEAN_BLUE("海洋蓝", Color(0xFF4A90FF), Color(0xFF0A0A0A), Color(0xFF121212)),
    SUNSET_ORANGE("日落橙", Color(0xFFFF9800), Color(0xFF0A0A0A), Color(0xFF121212)),
    PINK_LOVE("粉红", Color(0xFFE91E63), Color(0xFF0A0A0A), Color(0xFF121212)),
    MINT_GREEN("薄荷绿", Color(0xFF00BFA5), Color(0xFF0A0A0A), Color(0xFF121212)),
    WARM_GOLD("暖金", Color(0xFFFFD700), Color(0xFF0A0A0A), Color(0xFF121212))
}

/**
 * Custom accent color options.
 */
val customAccentColors = listOf(
    Color(0xFF1ED760), // Green
    Color(0xFF4A90FF), // Blue
    Color(0xFFFF3B30), // Red
    Color(0xFFFF9800), // Orange
    Color(0xFFBB86FC), // Purple
    Color(0xFF00BFA5), // Teal
    Color(0xFFE91E63), // Pink
    Color(0xFFFFD700), // Gold
    Color(0xFF00BCD4), // Cyan
    Color(0xFF8BC34A), // Light Green
)
