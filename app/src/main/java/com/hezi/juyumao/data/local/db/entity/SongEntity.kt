package com.hezi.juyumao.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "未知艺术家",
    val album: String = "未知专辑",
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
)
