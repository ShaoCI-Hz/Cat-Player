package com.hezi.juyumao.data.remote.smb

import com.hezi.juyumao.data.local.db.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmbFileScanner {

    private val audioExtensions = setOf(
        "mp3", "aac", "flac", "wav", "ogg", "opus",
        "dsf", "dff", "ape", "wv", "m4a", "wma",
    )

    suspend fun scanDirectory(
        smbClient: SmbClientWrapper,
        path: String,
        serverId: Long,
    ): Result<List<SongEntity>> = withContext(Dispatchers.IO) {
        try {
            val songs = mutableListOf<SongEntity>()
            scanRecursive(smbClient, path, serverId, songs)
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun scanRecursive(
        smbClient: SmbClientWrapper,
        path: String,
        serverId: Long,
        result: MutableList<SongEntity>,
    ) {
        val filesResult = smbClient.listFiles(path)
        if (filesResult.isFailure) return

        val files = filesResult.getOrNull() ?: return

        for (file in files) {
            if (file.isDirectory) {
                scanRecursive(smbClient, file.path, serverId, result)
            } else if (isAudioFile(file.name)) {
                result.add(
                    SongEntity(
                        title = extractTitle(file.name),
                        artist = "未知艺术家",
                        album = "未知专辑",
                        filePath = file.path,
                        fileSize = file.size,
                        mimeType = getMimeType(file.name),
                        isHiRes = isHiRes(file.name),
                        source = "SMB",
                        smbServerId = serverId,
                        smbSharePath = file.path,
                    )
                )
            }
        }
    }

    private fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in audioExtensions
    }

    private fun isHiRes(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("dsf", "dff", "flac", "ape", "wv")
    }

    private fun extractTitle(fileName: String): String {
        return fileName.substringBeforeLast('.').trim()
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "aac", "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "dsf", "dff" -> "audio/dsd"
            "ape" -> "audio/ape"
            "wv" -> "audio/wavpack"
            "wma" -> "audio/x-ms-wma"
            else -> "audio/*"
        }
    }
}
