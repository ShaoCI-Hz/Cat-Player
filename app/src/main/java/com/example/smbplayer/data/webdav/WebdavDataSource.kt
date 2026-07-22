package com.example.smbplayer.data.webdav

import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream

/**
 * Media3 DataSource for WebDAV streaming via OkHttp.
 * Supports seek via HTTP Range requests.
 */
class WebdavDataSource(
    private val url: String,
    private val username: String = "",
    private val password: String = "",
    private val fileSize: Long = 0
) : BaseDataSource(/* isNetwork = */ true) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        close()
        transferInitializing(dataSpec)

        try {
            val requestBuilder = Request.Builder().url(url).get()

            // Add basic auth if credentials provided
            if (username.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", Credentials.basic(username, password))
            }

            // Add Range header for seeking
            if (dataSpec.position > 0) {
                requestBuilder.addHeader("Range", "bytes=${dataSpec.position}-")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("WebDAV request failed: ${response.code}")
            }

            inputStream = response.body?.byteStream()
            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileSize - dataSpec.position
            }

            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
            close()
            throw IOException("WebDAV open failed: ${e.message}", e)
        }
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

    override fun getUri(): android.net.Uri? = android.net.Uri.parse(url)

    override fun close() {
        transferEnded()
        try { inputStream?.close() } catch (_: Exception) {}
        inputStream = null
        bytesRemaining = 0
    }
}
