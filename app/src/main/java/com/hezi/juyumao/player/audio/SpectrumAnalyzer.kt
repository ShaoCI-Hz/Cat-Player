package com.hezi.juyumao.player.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 频谱可视化数据源：绑定播放器的 audioSessionId，采集 FFT 数据。
 *
 * 播放页/均衡器页共用；仅在播放中采集（onDataCapture 回调由系统线程调用）。
 * 归一化到 0..1 的柱状值（默认 48 柱），供 Canvas 绘制。
 */
@Singleton
class SpectrumAnalyzer @Inject constructor() {

    private var visualizer: Visualizer? = null
    private var sessionId: Int = -1
    private var active = false

    private val _spectrum = MutableStateFlow(FloatArray(BAR_COUNT))
    val spectrum: StateFlow<FloatArray> = _spectrum.asStateFlow()

    companion object {
        const val BAR_COUNT = 48
        private const val CAPTURE_SIZE = 1024
    }

    /**
     * 绑定到指定 audioSessionId 并开始采集
     * @param enabled 用户设置是否开启频谱（false 时不启动采集，节省电量）
     */
    suspend fun start(sessionId: Int, enabled: Boolean = true) {
        if (sessionId < 0 || sessionId == this.sessionId && active) return
        stop()
        this.sessionId = sessionId
        if (!enabled) return
        withContext(Dispatchers.IO) {
            try {
                val v = Visualizer(sessionId)
                if (v.captureSize != Visualizer.getCaptureSizeRange()[1]) {
                    v.captureSize = CAPTURE_SIZE
                }
                v.setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                        override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (!active || fft == null) return
                            _spectrum.value = computeBars(fft)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2, // ~30fps
                    false, // waveform off, fft on
                    true,
                )
                v.enabled = true
                visualizer = v
                active = true
            } catch (_: Exception) {
                // 部分设备/会话不支持 Visualizer
                active = false
            }
        }
    }

    fun stop() {
        active = false
        try { visualizer?.enabled = false } catch (_: Exception) {}
        try { visualizer?.release() } catch (_: Exception) {}
        visualizer = null
        sessionId = -1
    }

    /** FFT 字节 → 归一化柱状值（对数刻度，低频权重更高） */
    private fun computeBars(fft: ByteArray): FloatArray {
        val bars = FloatArray(BAR_COUNT)
        if (fft.size < 4) return bars
        val usable = fft.size / 2 - 1 // 去掉 DC 和 Nyquist
        for (i in 0 until BAR_COUNT) {
            // 对数频段映射：低频更多柱
            val start = (usable.toFloat() * (i.toFloat() / BAR_COUNT).let { it * it }).toInt()
            val end = (usable.toFloat() * ((i + 1).toFloat() / BAR_COUNT).let { it * it }).toInt().coerceAtLeast(start + 1)
            var magnitude = 0.0
            for (j in start until end.coerceAtMost(usable)) {
                val real = fft[j * 2].toInt()
                val imag = fft[j * 2 + 1].toInt()
                magnitude += kotlin.math.sqrt((real * real + imag * imag).toDouble())
            }
            val avg = magnitude / (end - start).coerceAtLeast(1)
            // 归一化到 0..1（经验阈值）
            bars[i] = (avg / 2000.0).coerceIn(0.0, 1.0).toFloat()
        }
        return bars
    }
}
