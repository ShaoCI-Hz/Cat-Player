package com.hezi.juyumao.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "未知艺术家",
    val album: String = "未知专辑",
    val albumArtist: String? = null,
    val albumArtUri: String? = null,
    val duration: Long = 0L,
    val filePath: String,
    val fileSize: Long = 0L,
    val mimeType: String = "",
    val isHiRes: Boolean = false,
    val source: String = "LOCAL",
    val smbServerId: Long? = null,
    val smbSharePath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val playCount: Long = 0L,
    val lastPlayedAt: Long = 0L,
    // 元数据字段
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val year: Int = 0,
    val genre: String? = null,
    val composer: String? = null,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val bitsPerSample: Int = 0,
    val hasEmbeddedLyrics: Boolean = false,
    val hasExternalLyrics: Boolean = false,
)
