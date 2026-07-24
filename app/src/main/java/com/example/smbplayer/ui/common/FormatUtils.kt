package com.example.smbplayer.ui.common

/**
 * Shared utility functions for the UI layer.
 */

/**
 * Format milliseconds to "M:SS" string.
 */
fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/**
 * Format milliseconds to "M:SS" string, returning empty for zero/negative.
 */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return ""
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/**
 * Format byte size to human-readable string.
 */
fun formatSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1048576 -> "${b / 1024} KB"
    else -> "%.1f MB".format(b / 1048576.0)
}
