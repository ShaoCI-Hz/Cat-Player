package com.hezi.juyumao.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val name: String,
    val artist: String,
    val albumArtUri: String? = null,
    val songCount: Int = 0,
    val songs: List<Song> = emptyList(),
)
