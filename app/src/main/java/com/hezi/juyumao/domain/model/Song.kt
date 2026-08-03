package com.hezi.juyumao.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String = "未知艺术家",
    val album: String = "未知专辑",
    val albumArtUri: String? = null,
    val duration: Long = 0L, // milliseconds
    val filePath: String,
    val fileSize: Long = 0L,
    val mimeType: String = "",
    val isHiRes: Boolean = false,
    val source: SongSource = SongSource.LOCAL,
    val smbServerId: Long? = null,
    val smbSharePath: String? = null,
    // 音频规格（HiRes 徽标与播放页展示）
    val sampleRate: Int = 0,
    val bitsPerSample: Int = 0,
    val bitrate: Int = 0,
)

enum class SongSource {
    LOCAL, SMB
}
