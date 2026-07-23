package com.example.smbplayer.share

import android.content.Context
import android.graphics.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates lyrics poster images for sharing.
 */
@Singleton
class LyricsPosterRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Generate a lyrics poster with blurred background and selected lyrics.
     * @return File path of the generated PNG
     */
    suspend fun generateLyricsPoster(
        lyrics: List<String>,
        title: String,
        artist: String,
        coverBitmap: Bitmap?,
        theme: PosterTheme = PosterTheme.DARK
    ): String = withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        when (theme) {
            PosterTheme.DARK -> {
                val bgPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(Color.parseColor("#0A0A0A"), Color.parseColor("#1A1A2E")),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            PosterTheme.GRADIENT -> {
                val bgPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        intArrayOf(Color.parseColor("#667eea"), Color.parseColor("#764ba2")),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            PosterTheme.MINIMAL -> {
                canvas.drawColor(Color.WHITE)
            }
            PosterTheme.NEON -> {
                canvas.drawColor(Color.BLACK)
            }
        }

        // Blurred cover art background (if available)
        if (coverBitmap != null) {
            val blurred = Bitmap.createScaledBitmap(coverBitmap, 200, 200, false)
            val scaled = Bitmap.createScaledBitmap(blurred, width, height, true)
            val overlayPaint = Paint().apply {
                alpha = 40
            }
            canvas.drawBitmap(scaled, 0f, 0f, overlayPaint)
        }

        // Lyrics
        val maxLines = minOf(lyrics.size, 6)
        val startY = 500f
        val lineSpacing = 140f

        for (i in 0 until maxLines) {
            val lyricPaint = Paint().apply {
                color = when (theme) {
                    PosterTheme.DARK -> Color.WHITE
                    PosterTheme.GRADIENT -> Color.WHITE
                    PosterTheme.MINIMAL -> Color.parseColor("#191414")
                    PosterTheme.NEON -> Color.parseColor("#00FF41")
                }
                textSize = 52f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                setShadowLayer(8f, 0f, 0f, when (theme) {
                    PosterTheme.NEON -> Color.parseColor("#00FF41")
                    else -> Color.parseColor("#80000000")
                })
            }
            canvas.drawText(
                lyrics[i].take(25),
                width / 2f,
                startY + i * lineSpacing,
                lyricPaint
            )
        }

        // Track info footer
        val footerY = height - 250f

        // Divider line
        val linePaint = Paint().apply {
            color = when (theme) {
                PosterTheme.DARK -> Color.parseColor("#40FFFFFF")
                PosterTheme.GRADIENT -> Color.parseColor("#80FFFFFF")
                PosterTheme.MINIMAL -> Color.parseColor("#20000000")
                PosterTheme.NEON -> Color.parseColor("#4000FF41")
            }
            strokeWidth = 2f
        }
        canvas.drawLine(200f, footerY - 40f, (width - 200).toFloat(), footerY - 40f, linePaint)

        // Title
        val titlePaint = Paint().apply {
            color = when (theme) {
                PosterTheme.DARK, PosterTheme.GRADIENT -> Color.WHITE
                PosterTheme.MINIMAL -> Color.parseColor("#191414")
                PosterTheme.NEON -> Color.parseColor("#00FF41")
            }
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(title.take(30), width / 2f, footerY, titlePaint)

        // Artist
        val artistPaint = Paint().apply {
            color = when (theme) {
                PosterTheme.DARK, PosterTheme.GRADIENT -> Color.parseColor("#B3B3B3")
                PosterTheme.MINIMAL -> Color.parseColor("#757575")
                PosterTheme.NEON -> Color.parseColor("#80FF80")
            }
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(artist.take(40), width / 2f, footerY + 50f, artistPaint)

        // App watermark
        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#40FFFFFF")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Cat Player", width / 2f, height - 80f, watermarkPaint)

        // Save
        val file = File(context.cacheDir, "lyrics_poster_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        file.absolutePath
    }

    enum class PosterTheme {
        DARK, GRADIENT, MINIMAL, NEON
    }
}
