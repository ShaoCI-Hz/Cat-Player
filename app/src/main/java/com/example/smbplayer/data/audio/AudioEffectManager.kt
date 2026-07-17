package com.example.smbplayer.data.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectManager @Inject constructor() {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        release()
        equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
        virtualizer = Virtualizer(0, audioSessionId).apply { enabled = false }
    }

    fun release() {
        runCatching { equalizer?.enabled = false; equalizer?.release() }
        runCatching { bassBoost?.enabled = false; bassBoost?.release() }
        runCatching { virtualizer?.enabled = false; virtualizer?.release() }
        equalizer = null; bassBoost = null; virtualizer = null
    }

    val numberOfBands: Short get() = equalizer?.numberOfBands ?: 0
    fun getBandLevel(band: Short): Int = equalizer?.getBandLevel(band)?.toInt() ?: 0
    fun setBandLevel(band: Short, level: Int) { equalizer?.setBandLevel(band, level.toShort()) }
    fun getCenterFreq(band: Short): Int = equalizer?.getCenterFreq(band)?.toInt() ?: 0
    fun getCurrentPreset(): Short = equalizer?.currentPreset ?: 0
    fun getNumberOfPresets(): Short = equalizer?.numberOfPresets ?: 0
    fun getPresetName(preset: Short): String = equalizer?.getPresetName(preset) ?: ""
    fun usePreset(preset: Short) { equalizer?.usePreset(preset) }
    val isEqEnabled: Boolean get() = equalizer?.enabled ?: false
    fun setEqEnabled(enabled: Boolean) { equalizer?.enabled = enabled }
    fun getBandLevelRange(): IntArray = equalizer?.bandLevelRange?.let { if (it.size >= 2) shortArrayOf(it[0], it[1]) else null }?.map { it.toInt() }?.toIntArray() ?: intArrayOf(0, 0)

    val isBassBoostSupported: Boolean get() = bassBoost?.strengthSupported ?: false
    fun getBassBoostStrength(): Int = bassBoost?.getRoundedStrength()?.toInt() ?: 0
    fun setBassBoostStrength(strength: Int) { bassBoost?.setStrength(strength.toShort()) }
    val isBassBoostEnabled: Boolean get() = bassBoost?.enabled ?: false
    fun setBassBoostEnabled(enabled: Boolean) { bassBoost?.enabled = enabled }

    val isVirtualizerSupported: Boolean get() = virtualizer?.strengthSupported ?: false
    fun getVirtualizerStrength(): Int = virtualizer?.getRoundedStrength()?.toInt() ?: 0
    fun setVirtualizerStrength(strength: Int) { virtualizer?.setStrength(strength.toShort()) }
    val isVirtualizerEnabled: Boolean get() = virtualizer?.enabled ?: false
    fun setVirtualizerEnabled(enabled: Boolean) { virtualizer?.enabled = enabled }
}
