package com.example.smbplayer.share

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates beautiful share cards for social media.
 */
@Singleton
class ShareCardRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Generate a share card with album art, track info, and gradient background.
     * @return File path of the generated PNG
     */
    suspend fun generateShareCard(
        title: String,
        artist: String,
        album: String,
        coverBitmap: Bitmap?,
        playCount: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#1A1A2E"),
                    Color.parseColor("#16213E"),
                    Color.parseColor("#0F3460"),
                    Color.parseColor("#0A0A0A")
                ),
                floatArrayOf(0f, 0.3f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Album art (centered, 600x600)
        val artSize = 600f
        val artLeft = (width - artSize) / 2
        val artTop = 400f
        if (coverBitmap != null) {
            val scaled = Bitmap.createScaledBitmap(coverBitmap, artSize.toInt(), artSize.toInt(), true)
            val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
            val artPath = Path().apply {
                addRoundRect(artRect, 24f, 24f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(artPath)
            canvas.drawBitmap(scaled, artLeft, artTop, null)
            canvas.restore()

            // Shadow
            val shadowPaint = Paint().apply {
                color = Color.parseColor("#40000000")
                maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawRect(artLeft + 4, artTop + 4, artLeft + artSize + 4, artTop + artSize + 4, shadowPaint)
        } else {
            // Placeholder
            val placeholderPaint = Paint().apply {
                color = Color.parseColor("#1ED760")
                style = Paint.Style.FILL
            }
            val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
            canvas.drawRoundRect(artRect, 24f, 24f, placeholderPaint)

            val iconPaint = Paint().apply {
                color = Color.WHITE
                textSize = 120f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("♪", width / 2f, artTop + artSize / 2 + 40f, iconPaint)
        }

        // Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(title.take(30), width / 2f, artTop + artSize + 100f, titlePaint)

        // Artist
        val artistPaint = Paint().apply {
            color = Color.parseColor("#B3B3B3")
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(artist.take(40), width / 2f, artTop + artSize + 160f, artistPaint)

        // Album
        if (album.isNotEmpty()) {
            val albumPaint = Paint().apply {
                color = Color.parseColor("#80B3B3B3")
                textSize = 32f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(album.take(50), width / 2f, artTop + artSize + 210f, albumPaint)
        }

        // Play count
        if (playCount > 0) {
            val countPaint = Paint().apply {
                color = Color.parseColor("#1ED760")
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("已播放 $playCount 次", width / 2f, artTop + artSize + 280f, countPaint)
        }

        // App watermark
        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#40FFFFFF")
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Cat Player", width / 2f, height - 100f, watermarkPaint)

        // Save to file
        val file = File(context.cacheDir, "share_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        file.absolutePath
    }
}
