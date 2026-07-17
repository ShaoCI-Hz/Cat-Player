package com.example.smbplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ========== Spotify-style Dark Palette ==========

val SpotifyGreen = Color(0xFF1ED760)
val SpotifyBlack = Color(0xFF0A0A0A)
val SpotifySurface = Color(0xFF121212)
val SpotifyCard = Color(0xFF181818)
val SpotifyElevated = Color(0xFF242424)
val SpotifyWhite = Color(0xFFFFFFFF)
val SpotifyGray = Color(0xFFA0A0A0)
val SpotifyDimGray = Color(0xFF6A6A6A)
val SpotifyProgressTrack = Color(0xFF535353)
val SpotifyError = Color(0xFFF15E6C)

private val SpotifyScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    primaryContainer = Color(0xFF0D4A28),
    onPrimaryContainer = Color(0xFFA4F0C4),
    secondary = SpotifyGray,
    onSecondary = SpotifyBlack,
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFD0D0D0),
    tertiary = Color(0xFF8888CC),
    onTertiary = SpotifyBlack,
    error = SpotifyError,
    onError = SpotifyBlack,
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    surface = SpotifySurface,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifyCard,
    onSurfaceVariant = SpotifyGray,
    surfaceDim = Color(0xFF0A0A0A),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
    inverseSurface = SpotifyWhite,
    inverseOnSurface = SpotifyBlack,
    inversePrimary = Color(0xFF0D6B32),
    scrim = Color(0xFF000000),
)

// ========== Shapes ==========

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

// ========== Typography ==========

val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
)

enum class ThemeMode { Dark, System }

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val colorScheme = SpotifyScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
