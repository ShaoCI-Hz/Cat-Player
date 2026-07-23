package com.example.smbplayer.data.download

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Download manager for offline music playback.
 * Supports SMB/SFTP/WebDAV sources via HTTP range requests.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder().build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Active downloads
    private val _downloads = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadTask>> = _downloads.asStateFlow()

    // Download directory
    private val downloadDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CatPlayer").apply {
            mkdirs()
        }
    }

    /**
     * Start downloading a track from a URL.
     * @param url HTTP URL of the track
     * @param fileName Target filename
     * @param title Display title
     * @return Download ID
     */
    fun startDownload(url: String, fileName: String, title: String): String {
        val id = "${System.currentTimeMillis()}_$fileName"
        val targetFile = File(downloadDir, fileName)

        // Check if already downloaded
        if (targetFile.exists()) {
            return id
        }

        val task = DownloadTask(
            id = id,
            url = url,
            title = title,
            fileName = fileName,
            targetFile = targetFile,
            status = DownloadStatus.PENDING
        )

        _downloads.value = _downloads.value + (id to task)

        // Start download in background
        scope.launch {
            executeDownload(task)
        }

        return id
    }

    /**
     * Check if a track has been downloaded locally.
     */
    fun isDownloaded(fileName: String): Boolean {
        return File(downloadDir, fileName).exists()
    }

    /**
     * Get the local file path for a downloaded track.
     */
    fun getLocalPath(fileName: String): String? {
        val file = File(downloadDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Get download directory size in bytes.
     */
    fun getDownloadSize(): Long {
        return downloadDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Clear all downloads.
     */
    fun clearAll() {
        downloadDir.listFiles()?.forEach { it.delete() }
        _downloads.value = emptyMap()
    }

    private suspend fun executeDownload(task: DownloadTask) {
        try {
            // Update status to downloading
            updateTask(task.copy(status = DownloadStatus.DOWNLOADING, progress = 0f))

            val request = Request.Builder().url(task.url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                updateTask(task.copy(status = DownloadStatus.FAILED, error = "HTTP ${response.code}"))
                return
            }

            val body = response.body ?: run {
                updateTask(task.copy(status = DownloadStatus.FAILED, error = "Empty response"))
                return
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(task.targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Update progress
                        val progress = if (totalBytes > 0) {
                            downloadedBytes.toFloat() / totalBytes
                        } else 0f

                        updateTask(task.copy(
                            status = DownloadStatus.DOWNLOADING,
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        ))

                        // Check for cancellation
                        if (!scope.isActive) break
                    }
                }
            }

            // Download complete
            updateTask(task.copy(
                status = DownloadStatus.COMPLETED,
                progress = 1f,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes
            ))

        } catch (e: Exception) {
            updateTask(task.copy(
                status = DownloadStatus.FAILED,
                error = e.message ?: "Unknown error"
            ))
        }
    }

    private fun updateTask(task: DownloadTask) {
        _downloads.value = _downloads.value + (task.id to task)
    }

    fun release() {
        scope.cancel()
    }
}

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val fileName: String,
    val targetFile: File,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED
}
