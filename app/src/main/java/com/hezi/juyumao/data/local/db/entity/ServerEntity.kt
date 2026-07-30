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
)
