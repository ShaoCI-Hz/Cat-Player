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
)
