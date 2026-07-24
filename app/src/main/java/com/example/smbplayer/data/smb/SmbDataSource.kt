package com.example.smbplayer.data.smb

import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.InputStream

class SmbDataSource(
    private val inputStreamProvider: () -> InputStream,
    private val fileSize: Long,
    private val smbUri: String = "smb://unknown"
) : BaseDataSource(/* isNetwork = */ true) {

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        close()
        transferInitializing(dataSpec)
        inputStream = inputStreamProvider()

        val stream = inputStream ?: throw java.io.IOException("无法打开 SMB 文件流")

        // Loop skip until all bytes are consumed (skip() may return fewer bytes than requested)
        if (dataSpec.position > 0) {
            var remaining = dataSpec.position
            while (remaining > 0) {
                val skipped = stream.skip(remaining)
                if (skipped <= 0) {
                    close()
                    throw java.io.IOException("无法跳过 ${dataSpec.position} 字节 (还剩 $remaining)")
                }
                remaining -= skipped
            }
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (fileSize > 0) {
            fileSize - dataSpec.position
        } else {
            // BUG-PVM-05 fix: When fileSize is unknown, use a large value
            // and let read() return END_OF_INPUT when stream ends
            Long.MAX_VALUE
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

    override fun getUri(): android.net.Uri? = android.net.Uri.parse(smbUri)

    override fun close() {
        transferEnded()
        try { inputStream?.close() } catch (_: Exception) {}
        inputStream = null
        bytesRemaining = 0
    }
}
