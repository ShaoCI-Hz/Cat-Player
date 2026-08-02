package com.hezi.juyumao.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.hezi.juyumao.data.remote.smb.SmbClientWrapper
import com.hezi.juyumao.data.remote.smb.SmbStreamSource

/**
 * 为 SMB 文件创建 ExoPlayer MediaSource
 * @param metadata 歌曲元数据（标题/艺术家/封面），确保通知栏和系统媒体控件能显示
 */
fun createSmbMediaSource(
    smbClient: SmbClientWrapper,
    filePath: String,
    mimeType: String,
    metadata: MediaMetadata? = null,
): MediaSource {
    val factory = DataSource.Factory {
        SmbStreamSource(smbClient, filePath)
    }

    return ProgressiveMediaSource.Factory(factory)
        .createMediaSource(
            MediaItem.Builder()
                .setUri("smb://$filePath")
                .setMimeType(mimeType)
                .also { builder -> if (metadata != null) builder.setMediaMetadata(metadata) }
                .build()
        )
}
