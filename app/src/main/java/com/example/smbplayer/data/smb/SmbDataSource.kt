package com.example.smbplayer.data.smb

import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.InputStream

class SmbDataSource(
    private val inputStreamProvider: () -> InputStream,
    private val fileSize: Long
) : BaseDataSource(/* isNetwork = */ true) {

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        close()
        transferInitializing(dataSpec)
        inputStream = inputStreamProvider()

        val stream = inputStream ?: throw java.io.IOException("无法打开 SMB 文件流")

        if (dataSpec.position > 0) {
            val skipped: Long = stream.skip(dataSpec.position)
            if (skipped < dataSpec.position) {
                close()
                throw java.io.IOException("无法跳过 ${dataSpec.position} 字节")
            }
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            fileSize - dataSpec.position
        }

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val stream = inputStream ?: return C.RESULT_END_OF_INPUT
        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()
        val bytesRead = stream.read(buffer, offset, bytesToRead)

        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        bytesRemaining -= bytesRead.toLong()
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri() = null

    override fun close() {
        try { inputStream?.close() } catch (_: Exception) {}
        inputStream = null
        bytesRemaining = 0
    }
}
