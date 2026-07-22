package com.example.smbplayer.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

object PaletteUtil {
    fun extractDarkMuted(coverBytes: ByteArray?, fallback: Color = Color(0xFF1A1C1E)): Color {
        if (coverBytes == null) return fallback
        return try {
            val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size, BitmapFactory.Options().apply { inSampleSize = 4 })
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.darkMutedSwatch ?: palette.darkVibrantSwatch ?: palette.mutedSwatch
            if (swatch != null) Color(swatch.rgb) else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun extractVibrant(coverBytes: ByteArray?, fallback: Color = Color(0xFF3482FF)): Color {
        if (coverBytes == null) return fallback
        return try {
            val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size, BitmapFactory.Options().apply { inSampleSize = 4 })
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.mutedSwatch
            if (swatch != null) Color(swatch.rgb) else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun extractColors(coverBytes: ByteArray?, fallback: List<Color> = listOf(Color(0xFF1A1C2E), Color(0xFF2D1B69), Color(0xFF1A1A1A))): List<Color> {
        if (coverBytes == null) return fallback
        return try {
            val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size, BitmapFactory.Options().apply { inSampleSize = 4 })
            val palette = Palette.from(bitmap).generate()
            val colors = mutableListOf<Color>()
            palette.darkMutedSwatch?.let { colors.add(Color(it.rgb)) }
            palette.vibrantSwatch?.let { colors.add(Color(it.rgb)) }
            palette.darkVibrantSwatch?.let { colors.add(Color(it.rgb)) }
            palette.mutedSwatch?.let { colors.add(Color(it.rgb)) }
            colors.take(3).ifEmpty { fallback }
        } catch (_: Exception) {
            fallback
        }
    }
}
