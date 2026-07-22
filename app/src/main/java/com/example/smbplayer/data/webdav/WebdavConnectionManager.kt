package com.example.smbplayer.data.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class WebdavFileEntry(
    val href: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long
)

@Singleton
class WebdavConnectionManager @Inject constructor() {
    private val client = OkHttpClient.Builder().followRedirects(true).build()
    private var isConnected = false
    private var baseUrl = ""
    private var username = ""
    private var password = ""

    suspend fun connect(url: String, user: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
            baseUrl = url.trimEnd('/')
            username = user
            password = pass
            // Test connection with PROPFIND
            val request = buildRequest(baseUrl, "PROPFIND", "<?xml version=\"1.0\"?><D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>")
            val response = client.newCall(request).execute()
            isConnected = response.code in 200..299 || response.code == 207
            isConnected
        } catch (_: Exception) {
            isConnected = false
            false
        }
    }

    fun disconnect() {
        isConnected = false
        baseUrl = ""
        username = ""
        password = ""
    }

    suspend fun listDirectory(path: String): List<WebdavFileEntry> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
            val request = buildRequest(url, "PROPFIND", "<?xml version=\"1.0\"?><D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>")
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            parseWebdavResponse(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isConnected(): Boolean = isConnected
    fun getBaseUrl(): String = baseUrl

    private fun buildRequest(url: String, method: String, body: String? = null): Request {
        val builder = Request.Builder().url(url)
        if (username.isNotEmpty()) {
            builder.addHeader("Authorization", Credentials.basic(username, password))
        }
        builder.addHeader("Depth", "1")
        if (body != null) {
            builder.method(method, body.toRequestBody("text/xml".toMediaType()))
        } else {
            builder.method(method, null)
        }
        return builder.build()
    }

    private fun parseWebdavResponse(xml: String): List<WebdavFileEntry> {
        val entries = mutableListOf<WebdavFileEntry>()
        val hrefRegex = Regex("<D:href>([^<]+)</D:href>")
        val sizeRegex = Regex("<D:getcontentlength>(\\d+)</D:getcontentlength>")
        val collectionRegex = Regex("<D:resourcetype>\\s*<D:collection/>")

        val hrefs = hrefRegex.findAll(xml).map { it.groupValues[1] }.toList()
        val sizes = sizeRegex.findAll(xml).map { it.groupValues[1].toLongOrNull() ?: 0L }.toList()
        val isCollections = collectionRegex.findAll(xml).map { true }.toList()

        hrefs.forEachIndexed { index, href ->
            val name = href.trimEnd('/').substringAfterLast('/').let {
                java.net.URLDecoder.decode(it, "UTF-8")
            }
            val isDir = index < isCollections.size && isCollections[index]
            val size = if (index < sizes.size) sizes[index] else 0L

            if (name.isNotEmpty()) {
                entries.add(WebdavFileEntry(href, name, isDir, size))
            }
        }

        return entries.sortedWith(compareBy<WebdavFileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }
}
