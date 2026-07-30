package com.hezi.juyumao.data.repository

import com.hezi.juyumao.data.local.artwork.ArtworkCache
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.local.lyrics.LyricsManager
import com.hezi.juyumao.data.local.metadata.AudioMetadata
import com.hezi.juyumao.data.local.metadata.MetadataExtractor
import com.hezi.juyumao.player.audio.LyricsData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 元数据统一入口，供 UI 层调用
 */
@Singleton
class MetadataRepository @Inject constructor(
    private val metadataExtractor: MetadataExtractor,
    private val lyricsManager: LyricsManager,
    private val artworkCache: ArtworkCache,
) {
    /**
     * 提取并缓存封面，返回缓存路径
     */
    suspend fun extractAndCacheArtwork(song: SongEntity): String? {
        // 已有缓存
        artworkCache.getArtworkPath(song.id)?.let { return it }

        // 提取元数据
        val meta = try {
            metadataExtractor.extract(song.filePath)
        } catch (_: Exception) {
            return null
        }

        // 缓存封面
        val artworkData = meta.artworkData ?: return null
        return artworkCache.saveArtwork(song.id, artworkData)
    }

    /**
     * 获取歌词
     */
    suspend fun getLyrics(song: SongEntity): LyricsData? {
        return lyricsManager.getLyrics(song)
    }

    /**
     * 提取完整元数据
     */
    suspend fun extractMetadata(filePath: String): AudioMetadata {
        return metadataExtractor.extract(filePath)
    }

    /**
     * 获取封面路径（优先缓存，否则提取）
     */
    fun getCachedArtworkPath(songId: Long): String? {
        return artworkCache.getArtworkPath(songId)
    }
}
