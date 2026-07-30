package com.hezi.juyumao.data.local.scanner

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.hezi.juyumao.data.local.db.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        // 文件扩展名白名单
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "aac", "m4a", "ogg", "opus", "wma", "wav", "flac",
            "dsf", "dff", "ape", "wv", "aiff", "aif",
        )

        // Hi-Res 格式扩展名
        private val HIRES_EXTENSIONS = setOf("dsf", "dff", "ape", "wv", "aiff", "aif", "flac")

        // 排除的目录关键词（路径中包含这些的都不是用户音乐）
        private val EXCLUDED_DIR_PATTERNS = listOf(
            "/Notifications/",
            "/Ringtones/",
            "/Alarms/",
            "/Recordings/",
            "/Voice Recorder/",
            "/Android/data/",
            "/Android/obb/",
            "/.微信/",
            "/Tencent/",
            "/tencent/",
            "/WhatsApp/",
            "/com.",
        )

        // 排除的文件名关键词
        private val EXCLUDED_FILENAME_KEYWORDS = listOf(
            "ringtone", "notification", "alarm", "voice_record",
            "wechat", "微信语音",
        )

        // 最小时长：30秒
        private const val MIN_DURATION_MS = 30_000L

        // 最小文件大小：100KB
        private const val MIN_FILE_SIZE_BYTES = 100_000L

        // 批量插入每批数量
        private const val BATCH_SIZE = 200
    }

    /**
     * 扫描设备上所有本地音乐，返回 SongEntity 列表
     */
    suspend fun scanAllMusic(): Result<List<SongEntity>> = withContext(Dispatchers.IO) {
        try {
            val songs = mutableListOf<SongEntity>()
            val seenPaths = HashSet<String>() // 去重用

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.IS_RINGTONE,
                MediaStore.Audio.Media.IS_ALARM,
                MediaStore.Audio.Media.IS_NOTIFICATION,
            )

            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val isMusicCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)
                val isRingtoneCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE)
                val isAlarmCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_ALARM)
                val isNotifCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION)

                while (cursor.moveToNext()) {
                    val filePath = cursor.getString(dataCol) ?: continue
                    val duration = cursor.getLong(durationCol)
                    val fileSize = cursor.getLong(sizeCol)
                    val title = cursor.getString(titleCol)
                    val isMusic = cursor.getInt(isMusicCol)
                    val isRingtone = cursor.getInt(isRingtoneCol)
                    val isAlarm = cursor.getInt(isAlarmCol)
                    val isNotif = cursor.getInt(isNotifCol)

                    // 过滤判断
                    if (!isValidMusicFile(
                            filePath = filePath,
                            duration = duration,
                            fileSize = fileSize,
                            title = title,
                            isMusic = isMusic,
                            isRingtone = isRingtone,
                            isAlarm = isAlarm,
                            isNotification = isNotif,
                        )
                    ) continue

                    // 按路径去重
                    if (!seenPaths.add(filePath.lowercase())) continue

                    val artist = cursor.getString(artistCol) ?: "未知艺术家"
                    val album = cursor.getString(albumCol) ?: "未知专辑"
                    val albumId = cursor.getLong(albumIdCol)
                    val mimeType = cursor.getString(mimeCol) ?: inferMimeType(filePath)
                    val albumArtUri = getAlbumArtUri(albumId)

                    songs.add(
                        SongEntity(
                            title = title ?: filePath.substringAfterLast('/').substringBeforeLast('.'),
                            artist = artist,
                            album = album,
                            albumArtUri = albumArtUri,
                            duration = duration,
                            filePath = filePath,
                            fileSize = fileSize,
                            mimeType = mimeType,
                            isHiRes = isHiRes(filePath),
                            source = "LOCAL",
                            smbServerId = null,
                            smbSharePath = null,
                        )
                    )
                }
            }

            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 过滤判断：是否是有效音乐文件
     */
    private fun isValidMusicFile(
        filePath: String,
        duration: Long,
        fileSize: Long,
        title: String?,
        isMusic: Int,
        isRingtone: Int,
        isAlarm: Int,
        isNotification: Int,
    ): Boolean {
        // 基础过滤：必须被系统标记为音乐
        if (isMusic != 1) return false

        // 排除铃声/闹钟/通知（被系统同时标记为这些的）
        if (isRingtone == 1 && isMusic != 1) return false
        if (isAlarm == 1 && isMusic != 1) return false
        if (isNotification == 1 && isMusic != 1) return false

        // 扩展名白名单
        val ext = filePath.substringAfterLast('.', "").lowercase()
        if (ext !in AUDIO_EXTENSIONS) return false

        // 时长过滤：< 30秒不是歌曲
        if (duration < MIN_DURATION_MS) return false

        // 文件大小过滤：< 100KB 不是完整歌曲
        if (fileSize < MIN_FILE_SIZE_BYTES) return false

        // 路径过滤：排除系统/应用目录
        val pathLower = filePath.lowercase()
        for (pattern in EXCLUDED_DIR_PATTERNS) {
            if (pathLower.contains(pattern.lowercase())) return false
        }

        // 文件名关键词过滤
        val fileName = filePath.substringAfterLast('/').lowercase()
        for (keyword in EXCLUDED_FILENAME_KEYWORDS) {
            if (fileName.contains(keyword.lowercase())) return false
        }

        return true
    }

    /**
     * 判断是否 Hi-Res
     */
    private fun isHiRes(filePath: String, sampleRate: Int? = null): Boolean {
        // 按采样率判断
        if (sampleRate != null && sampleRate > 44100) return true
        // 按扩展名判断
        val ext = filePath.substringAfterLast('.', "").lowercase()
        return ext in HIRES_EXTENSIONS
    }

    /**
     * 提取专辑封面 URI
     */
    private fun getAlbumArtUri(albumId: Long): String? {
        return try {
            val uri = Uri.parse("content://media/external/audio/albumart")
            Uri.withAppendedPath(uri, albumId.toString()).toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 从文件路径推断 MIME 类型
     */
    private fun inferMimeType(filePath: String): String {
        val ext = filePath.substringAfterLast('.', "").lowercase()
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
            "aiff", "aif" -> "audio/aiff"
            else -> "audio/*"
        }
    }
}
