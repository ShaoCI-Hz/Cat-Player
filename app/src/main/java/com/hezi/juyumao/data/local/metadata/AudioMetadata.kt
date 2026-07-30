package com.hezi.juyumao.data.local.metadata

import com.hezi.juyumao.player.audio.LyricLine

/**
 * 音频文件元数据统一数据类
 */
data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val trackNumber: Int? = null,
    val totalTracks: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val composer: String? = null,
    val comment: String? = null,
    val duration: Long = 0L,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val bitsPerSample: Int = 0,
    val channels: Int = 0,
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val artworkData: ByteArray? = null,
    val artworkMimeType: String? = null,
    val embeddedLyrics: String? = null,
    val syncedLyrics: List<LyricLine>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioMetadata) return false
        return title == other.title && artist == other.artist && album == other.album
    }
    override fun hashCode(): Int = 31 * (title.hashCode() + artist.hashCode() + album.hashCode())
}
