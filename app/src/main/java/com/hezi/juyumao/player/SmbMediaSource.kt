package com.hezi.juyumao.player

import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.hezi.juyumao.data.remote.smb.SmbClientWrapper
import com.hezi.juyumao.data.remote.smb.SmbStreamSource

class SmbDataSourceFactory(
    private val smbClient: SmbClientWrapper,
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return SmbStreamSource(smbClient, "")
    }
}

fun createSmbMediaSource(
    smbClient: SmbClientWrapper,
    filePath: String,
    mimeType: String,
): MediaSource {
    val factory = object : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return SmbStreamSource(smbClient, filePath)
        }
    }

    return ProgressiveMediaSource.Factory(factory)
        .createMediaSource(
            MediaItem.Builder()
                .setUri("smb://$filePath")
                .setMimeType(mimeType)
                .build()
        )
}
