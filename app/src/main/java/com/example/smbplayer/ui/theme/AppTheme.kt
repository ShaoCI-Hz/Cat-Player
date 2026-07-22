package com.example.smbplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ========== Dark Palette ==========

val CatPlayerGreen = Color(0xFF1ED760)
val CatPlayerBlack = Color(0xFF0A0A0A)
val CatPlayerSurface = Color(0xFF121212)
val CatPlayerCard = Color(0xFF181818)
val CatPlayerElevated = Color(0xFF242424)
val CatPlayerWhite = Color(0xFFFFFFFF)
val CatPlayerGray = Color(0xFFA0A0A0)
val CatPlayerDimGray = Color(0xFF6A6A6A)
val CatPlayerProgressTrack = Color(0xFF535353)
val CatPlayerError = Color(0xFFF15E6C)

private val DarkScheme = darkColorScheme(
    primary = CatPlayerGreen,
    onPrimary = CatPlayerBlack,
    primaryContainer = Color(0xFF0D4A28),
    onPrimaryContainer = Color(0xFFA4F0C4),
    secondary = CatPlayerGray,
    onSecondary = CatPlayerBlack,
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFD0D0D0),
    tertiary = Color(0xFF8888CC),
    onTertiary = CatPlayerBlack,
    error = CatPlayerError,
    onError = CatPlayerBlack,
    background = CatPlayerBlack,
    onBackground = CatPlayerWhite,
    surface = CatPlayerSurface,
    onSurface = CatPlayerWhite,
    surfaceVariant = CatPlayerCard,
    onSurfaceVariant = CatPlayerGray,
    surfaceDim = Color(0xFF0A0A0A),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLow = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
    inverseSurface = CatPlayerWhite,
    inverseOnSurface = CatPlayerBlack,
    inversePrimary = Color(0xFF0D6B32),
    scrim = Color(0xFF000000),
)

// ========== Light Palette ==========

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0CC),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF6B6B6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF6B6BAA),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191414),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF191414),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF757575),
    surfaceDim = Color(0xFFF0F0F0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE8E8E8),
    inverseSurface = Color(0xFF2A2A2A),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFF00A843),
    scrim = Color(0xFF000000),
)

// ========== Shapes ==========

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
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

enum class ThemeMode { Dark, Light, System }

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
