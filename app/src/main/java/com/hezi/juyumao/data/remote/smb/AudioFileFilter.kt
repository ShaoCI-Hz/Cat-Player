package com.hezi.juyumao.data.remote.smb

import com.hezi.juyumao.data.local.metadata.HiRes

/**
 * 音频文件过滤公共工具类
 * 本地扫描和 SMB 扫描共用同一套过滤规则
 */
object AudioFileFilter {

    // 文件扩展名白名单
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "aac", "m4a", "ogg", "opus", "wma", "wav", "flac",
        "dsf", "dff", "ape", "wv", "aiff", "aif",
    )

    // 排除的目录关键词
    private val EXCLUDED_DIR_PATTERNS = listOf(
        "/Notifications/", "/Ringtones/", "/Alarms/",
        "/Recordings/", "/Voice Recorder/",
        "/Android/data/", "/Android/obb/",
        "/.微信/", "/Tencent/", "/tencent/",
        "/WhatsApp/", "/com.",
    )

    // 排除的文件名关键词
    private val EXCLUDED_FILENAME_KEYWORDS = listOf(
        "ringtone", "notification", "alarm", "voice_record",
        "wechat", "微信语音",
    )

    /** 判断文件是否是音频文件（按扩展名） */
    fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }

    /** 判断路径是否应被排除（系统/应用目录） */
    fun isExcludedPath(path: String): Boolean {
        val lower = path.lowercase()
        return EXCLUDED_DIR_PATTERNS.any { lower.contains(it.lowercase()) }
    }

    /** 判断文件名是否应被排除（铃声/语音等关键词） */
    fun isExcludedFileName(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return EXCLUDED_FILENAME_KEYWORDS.any { lower.contains(it.lowercase()) }
    }

    /**
     * 判断是否 Hi-Res（SMB 扫描无采样率/位深信息，只能按 DSD 扩展名判定；
     * 元数据批量缓存后由 MetadataRepository 用完整信息刷新 isHiRes）
     */
    fun isHiRes(fileName: String): Boolean = HiRes.isHiRes(fileExtension = fileName)

    /** 从文件名提取标题（去掉扩展名） */
    fun extractTitle(fileName: String): String {
        return fileName.substringBeforeLast('.').trim()
    }

    /** 推断 MIME 类型 */
    fun getMimeType(fileName: String): String {
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
            "aiff", "aif" -> "audio/aiff"
            else -> "audio/*"
        }
    }

    /**
     * 从文件路径推断元数据（艺术家/专辑/标题）
     * NAS 上的音乐通常按 "艺术家/专辑/曲目.mp3" 组织
     */
    fun inferMetadata(filePath: String): Triple<String, String, String> {
        val parts = filePath.split("/").filter { it.isNotEmpty() }
        return when {
            parts.size >= 3 -> Triple(
                parts[parts.size - 3],          // artist
                parts[parts.size - 2],          // album
                parts[parts.size - 1].substringBeforeLast('.').trim() // title
            )
            parts.size == 2 -> Triple(
                parts[0],
                "未知专辑",
                parts[1].substringBeforeLast('.').trim()
            )
            else -> Triple(
                "未知艺术家",
                "未知专辑",
                filePath.substringAfterLast('/').substringBeforeLast('.').trim()
            )
        }
    }
}
