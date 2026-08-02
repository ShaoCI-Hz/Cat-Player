package com.hezi.juyumao.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ip: String,
    val port: Int = 445,
    val username: String = "",
    val password: String = "",
    val shareName: String = "",
    val musicPath: String = "/",
    val lastConnectedAt: Long = 0L,
    val autoConnect: Boolean = true,
    val lastScanAt: Long = 0L,
    val scannedSongCount: Int = 0,
    val connectionError: String? = null,
    val isOnline: Boolean = false,
) {
    /**
     * 真正的共享名（shareName 可能存了完整路径如 "共享名/子目录"，取第一段）
     * 兼容旧版本数据
     */
    val effectiveShareName: String
        get() = shareName.split("/", limit = 2)[0].trim()

    override fun toString(): String =
        "ServerEntity(id=$id, name=$name, ip=$ip, port=$port, username=$username, password=***, shareName=$shareName)"
}
