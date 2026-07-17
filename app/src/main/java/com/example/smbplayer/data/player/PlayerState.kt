package com.example.smbplayer.data.player

sealed class PlayerState {
    data object Idle : PlayerState()
    data object Loading : PlayerState()
    data class Playing(val track: TrackInfo) : PlayerState()
    data class Paused(val track: TrackInfo) : PlayerState()
    data class Error(val track: TrackInfo?, val message: String) : PlayerState()
}

enum class TrackSource { LOCAL, SMB }

data class TrackInfo(
    val source: TrackSource,
    val title: String,
    val artist: String = "未知艺术家",
    val album: String = "未知专辑",
    val durationMs: Long = 0,
    val localUri: String? = null,
    val smbPath: String = "",
    val fileSize: Long = 0,
    val coverArtBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackInfo) return false
        return source == other.source && title == other.title && artist == other.artist &&
            album == other.album && durationMs == other.durationMs && localUri == other.localUri &&
            smbPath == other.smbPath && fileSize == other.fileSize
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + smbPath.hashCode()
        result = 31 * result + (localUri?.hashCode() ?: 0)
        return result
    }
}

enum class PlayMode {
    Sequential,
    Random,
    Single,
    Loop
}
