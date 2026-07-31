package com.hezi.juyumao.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SmbServer(
    val id: Long = 0,
    val name: String,
    val ip: String,
    val port: Int = 445,
    val username: String = "",
    val password: String = "",
    val shareName: String = "",
    val musicPath: String = "/",
    val isConnected: Boolean = false,
    val lastConnectedAt: Long = 0L,
) {
    // LOW: 防止密码泄露到日志
    override fun toString(): String =
        "SmbServer(id=$id, name=$name, ip=$ip, port=$port, username=$username, password=***, shareName=$shareName)"
}
