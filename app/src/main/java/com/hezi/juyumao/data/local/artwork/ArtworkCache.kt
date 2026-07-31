package com.hezi.juyumao.data.local.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheDir = File(context.cacheDir, "album_art").apply { mkdirs() }

    // HIGH: 基于内存大小的 LruCache，避免 OOM
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 8).toInt()
    private val memoryCache = object : LruCache<String, Bitmap>(maxMemory) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun getArtworkPath(songId: Long): String? {
        val file = File(cacheDir, "art_$songId.jpg")
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    fun saveArtwork(songId: Long, artworkData: ByteArray): String {
        val file = File(cacheDir, "art_$songId.jpg")
        val tmp = File(cacheDir, "art_$songId.tmp")
        try {
            tmp.writeBytes(artworkData)
            tmp.renameTo(file)
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
        // HIGH: 自动清理磁盘缓存
        cleanup()
        return file.absolutePath
    }

    fun getBitmap(path: String): Bitmap? = memoryCache.get(path)

    fun putBitmap(path: String, bitmap: Bitmap) = memoryCache.put(path, bitmap)

    fun decodeBitmap(path: String): Bitmap? {
        getBitmap(path)?.let { return it }
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        putBitmap(path, bitmap)
        return bitmap
    }

    fun cleanup(maxSizeBytes: Long = 100L * 1024 * 1024) {
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var totalSize = files.sumOf { it.length() }
        for (file in files) {
            if (totalSize <= maxSizeBytes) break
            totalSize -= file.length()
            file.delete()
        }
    }
}
