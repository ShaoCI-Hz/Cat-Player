package com.hezi.juyumao.data.local.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 封面图片缓存管理（磁盘 + 内存 LRU）
 */
@Singleton
class ArtworkCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheDir = File(context.cacheDir, "album_art").apply { mkdirs() }
    private val memoryCache = object : LruCache<String, Bitmap>(30) {
        override fun sizeOf(key: String, value: Bitmap) = 1
    }

    /** 获取封面文件路径（磁盘缓存命中时返回路径） */
    fun getArtworkPath(songId: Long): String? {
        val file = File(cacheDir, "art_$songId.jpg")
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    /** 保存封面二进制数据到磁盘缓存 */
    fun saveArtwork(songId: Long, artworkData: ByteArray): String {
        val file = File(cacheDir, "art_$songId.jpg")
        file.writeBytes(artworkData)
        return file.absolutePath
    }

    /** 从内存缓存获取 Bitmap */
    fun getBitmap(path: String): Bitmap? = memoryCache.get(path)

    /** 存入内存缓存 */
    fun putBitmap(path: String, bitmap: Bitmap) = memoryCache.put(path, bitmap)

    /** 从文件路径解码 Bitmap（带内存缓存） */
    fun decodeBitmap(path: String): Bitmap? {
        getBitmap(path)?.let { return it }
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        putBitmap(path, bitmap)
        return bitmap
    }

    /** 清理过期缓存（超过 100MB 时清理最旧的） */
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
