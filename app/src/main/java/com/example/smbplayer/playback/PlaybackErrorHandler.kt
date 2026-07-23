package com.example.smbplayer.playback

import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles playback errors with automatic retry and user-friendly messages.
 */
@Singleton
class PlaybackErrorHandler @Inject constructor() {

    private var retryCount = 0
    private val maxRetries = 3
    private val baseDelayMs = 1000L

    data class PlaybackError(
        val message: String,
        val isRetryable: Boolean,
        val action: ErrorAction
    )

    enum class ErrorAction {
        RETRY, SKIP, GO_OFFLINE, SHOW_ERROR
    }

    /**
     * Handle a playback error and determine the action.
     */
    fun handleError(error: Throwable): PlaybackError {
        val message = error.message ?: "Unknown error"
        val isRetryable = isRetryableError(error)

        return when {
            isRetryable && retryCount < maxRetries -> {
                retryCount++
                PlaybackError(
                    message = "播放失败，正在重试 ($retryCount/$maxRetries)...",
                    isRetryable = true,
                    action = ErrorAction.RETRY
                )
            }
            isRetryable -> {
                retryCount = 0
                PlaybackError(
                    message = "播放失败，请检查网络连接",
                    isRetryable = false,
                    action = ErrorAction.SKIP
                )
            }
            else -> {
                retryCount = 0
                PlaybackError(
                    message = "无法播放: $message",
                    isRetryable = false,
                    action = ErrorAction.SHOW_ERROR
                )
            }
        }
    }

    /**
     * Get delay before retry (exponential backoff).
     */
    fun getRetryDelay(): Long {
        return baseDelayMs * (1L shl (retryCount - 1).coerceAtMost(3))
    }

    /**
     * Reset retry counter on successful playback.
     */
    fun resetRetryCount() {
        retryCount = 0
    }

    private fun isRetryableError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("network") ||
               message.contains("socket") ||
               message.contains("eof") ||
               message.contains("reset")
    }
}
