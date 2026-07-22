package com.example.smbplayer.data.audio

import android.media.audiofx.Equalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Equalizer with up to 30-band support.
 * Falls back to hardware-supported band count if less than 30.
 */
@Singleton
class EnhancedEqualizer @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var _bandCount = 0

    // Standard 31-band ISO frequencies (Hz)
    private val isoFrequencies = intArrayOf(
        20, 25, 31, 40, 50, 63, 80, 100, 125, 160,
        200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600,
        2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000
    )

    // Preset names
    val presets = listOf(
        "Flat", "Bass Boost", "Treble Boost", "Vocal",
        "Rock", "Pop", "Jazz", "Classical", "Electronic", "Dance"
    )

    // Preset values (gain in millibels for each band, 0-29)
    private val presetValues = mapOf(
        0 to IntArray(30) { 0 },                                    // Flat
        1 to IntArray(30) { i -> if (i < 10) 600 else 0 },         // Bass Boost
        2 to IntArray(30) { i -> if (i >= 20) 600 else 0 },        // Treble Boost
        3 to IntArray(30) { i -> if (i in 10..20) 400 else 0 },    // Vocal
        4 to intArrayOf(500, 400, 300, 0, -100, -200, -200, -100, 0, 200, 300, 400, 500, 500, 400, 300, 200, 100, 0, 0, 100, 200, 300, 400, 500, 500, 400, 300, 200, 100), // Rock
        5 to intArrayOf(-100, -100, 0, 200, 400, 400, 200, 0, -100, -200, -200, -100, 0, 200, 400, 400, 200, 0, -100, -200, -200, -100, 0, 200, 400, 400, 200, 0, -100, -200), // Pop
        6 to intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // Jazz (placeholder)
        7 to intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // Classical (placeholder)
        8 to IntArray(30) { i -> if (i in 5..15) 500 else if (i > 20) 300 else 0 }, // Electronic
        9 to IntArray(30) { i -> if (i < 5) 500 else if (i in 10..20) 300 else 0 }  // Dance
    )

    fun attach(audioSessionId: Int) {
        release()
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                _bandCount = numberOfBands.toInt()
            }
        } catch (_: Exception) {
            _bandCount = 0
        }
    }

    fun release() {
        runCatching { equalizer?.enabled = false; equalizer?.release() }
        equalizer = null
        _bandCount = 0
    }

    val bandCount: Int get() = _bandCount
    val maxBands: Int get() = 30

    fun getBandFrequency(band: Int): Int {
        if (band < isoFrequencies.size) return isoFrequencies[band]
        return equalizer?.getCenterFreq(band.toShort())?.div(1000) ?: 0
    }

    fun getBandLevel(band: Int): Int {
        return equalizer?.getBandLevel(band.toShort())?.toInt() ?: 0
    }

    fun setBandLevel(band: Int, level: Int) {
        equalizer?.setBandLevel(band.toShort(), level.toShort().coerceIn(
            equalizer?.bandLevelRange?.get(0) ?: -1500,
            equalizer?.bandLevelRange?.get(1) ?: 1500
        ))
    }

    fun getBandLevelRange(): IntArray {
        return equalizer?.bandLevelRange?.map { it.toInt() }?.toIntArray() ?: intArrayOf(-1500, 1500)
    }

    fun applyPreset(presetIndex: Int) {
        val values = presetValues[presetIndex] ?: return
        for (i in 0 until minOf(bandCount, values.size)) {
            setBandLevel(i, values[i])
        }
    }

    fun getPresetName(index: Int): String {
        return presets.getOrElse(index) { "Unknown" }
    }

    val isEnabled: Boolean get() = equalizer?.enabled ?: false
    fun setEnabled(enabled: Boolean) { equalizer?.enabled = enabled }

    /**
     * Get band index closest to a given frequency (Hz).
     */
    fun getBandForFrequency(frequency: Int): Int {
        var closest = 0
        var minDiff = Int.MAX_VALUE
        for (i in 0 until minOf(bandCount, isoFrequencies.size)) {
            val diff = kotlin.math.abs(isoFrequencies[i] - frequency)
            if (diff < minDiff) {
                minDiff = diff
                closest = i
            }
        }
        return closest
    }
}

/**
 * Parametric EQ band configuration.
 */
data class ParametricBand(
    val frequency: Float,   // Center frequency in Hz
    val gain: Float,        // Gain in dB (-12 to +12)
    val q: Float            // Quality factor (0.5 to 10)
)

/**
 * Parametric Equalizer for advanced audio tuning.
 * Note: Android's built-in Equalizer doesn't support parametric mode.
 * This class provides the configuration model for future custom AudioProcessor implementation.
 */
@Singleton
class ParametricEqualizer @Inject constructor() {
    private val bands = mutableListOf<ParametricBand>()

    fun getBands(): List<ParametricBand> = bands.toList()

    fun setBand(index: Int, frequency: Float, gain: Float, q: Float) {
        while (bands.size <= index) {
            bands.add(ParametricBand(1000f, 0f, 1f))
        }
        bands[index] = ParametricBand(
            frequency = frequency.coerceIn(20f, 20000f),
            gain = gain.coerceIn(-12f, 12f),
            q = q.coerceIn(0.5f, 10f)
        )
    }

    fun addBand(frequency: Float = 1000f, gain: Float = 0f, q: Float = 1f) {
        bands.add(ParametricBand(frequency, gain, q))
    }

    fun removeBand(index: Int) {
        if (index in bands.indices) bands.removeAt(index)
    }

    fun clear() { bands.clear() }

    fun getBandCount(): Int = bands.size
}
