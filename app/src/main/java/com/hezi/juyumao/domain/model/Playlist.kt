package com.hezi.juyumao.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: Long = 0,
    val name: String,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
