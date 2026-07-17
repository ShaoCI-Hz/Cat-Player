package com.example.smbplayer.data.smb

data class SmbConfig(
    val host: String,
    val shareName: String,
    val username: String,
    val password: String,
    val domain: String = ""
)

data class SmbFileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0
)

enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Error
}

class SmbNotConnectedException : Exception("SMB 未连接或连接已断开")
