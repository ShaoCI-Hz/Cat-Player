package com.hezi.juyumao.data.remote.smb

import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream

class SmbStreamSource(
    private val smbClient: SmbClient,
    private val filePath: String,
) : BaseDataSource(/* isNetwork= */ true) {

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        val result = runBlocking(Dispatchers.IO) {
            smbClient.openFile(filePath)
        }
        inputStream = result.getOrNull()
            ?: throw Exception("无法打开文件: $filePath")

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            Long.MAX_VALUE
        }

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return -1

        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()
        val bytesRead = inputStream?.read(buffer, offset, bytesToRead) ?: -1

        if (bytesRead == -1) {
            bytesRemaining = 0
            return -1
        }

        bytesRemaining -= bytesRead
        return bytesRead
    }

    override fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        inputStream = null
    }
}
