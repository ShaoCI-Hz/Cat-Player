package com.hezi.juyumao.player

import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 动态缓冲 LoadControl：根据用户设置的缓冲大小与当前歌曲是否 HiRes 分档。
 *
 * Media3 的 LoadControl 在构建播放器时固定，无法运行时替换，因此用委托模式
 * 在 shouldStartPlayback 时按当前档位动态计算最低缓冲时长：
 * - 普通歌曲：默认 1.5s（对应 256KB 设置）
 * - HiRes 歌曲：翻倍预缓冲，缓解大文件 SMB 串流卡顿
 */
@Singleton
class DynamicLoadControl @Inject constructor() : LoadControl by delegate {

    companion object {
        private val delegate: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DEFAULT_MIN_BUFFER_MS,
                DEFAULT_MAX_BUFFER_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .build()

        private const val DEFAULT_MIN_BUFFER_MS = 50_000
        private const val DEFAULT_MAX_BUFFER_MS = 200_000
        private const val DEFAULT_BUFFER_FOR_PLAYBACK_MS = 1_500
        private const val DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_500
        private const val HIRES_MULTIPLIER = 2
    }

    @Volatile
    private var playbackBufferMs: Int = DEFAULT_BUFFER_FOR_PLAYBACK_MS

    @Volatile
    private var isHiResActive: Boolean = false

    /**
     * 由 PlaybackController 在每次播放前调用
     * @param userBufferKb 用户设置的缓冲大小（KB），0 表示默认
     * @param isHiRes 当前歌曲是否 HiRes
     */
    fun updateSettings(userBufferKb: Int, isHiRes: Boolean) {
        // 用户设置单位 KB，换算成播放缓冲毫秒（256KB ≈ 1.5s @320kbps）
        val baseMs = if (userBufferKb > 0) (userBufferKb * 6).coerceIn(1_500, 30_000) else DEFAULT_BUFFER_FOR_PLAYBACK_MS
        playbackBufferMs = if (isHiRes) baseMs * HIRES_MULTIPLIER else baseMs
        isHiResActive = isHiRes
    }

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean {
        // HiRes 歌曲需要更多预缓冲才启动播放，避免 SMB 串流起播即卡
        val requiredUs = playbackBufferMs * 1000L
        if (parameters.bufferedDurationUs < requiredUs) return false
        return delegate.shouldStartPlayback(parameters)
    }

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        // HiRes 歌曲持续加载到目标缓冲，减少中断
        if (isHiResActive && parameters.bufferedDurationUs < playbackBufferMs * 2L * 1000L) return true
        return delegate.shouldContinueLoading(parameters)
    }
}
