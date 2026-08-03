package com.hezi.juyumao.player.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerBand(
    val index: Short,
    val centerFreq: Int,      // in milliHz
    val minLevel: Short,
    val maxLevel: Short,
    val currentLevel: Short,
)

data class EqualizerPreset(
    val index: Short,
    val name: String,
)

data class EqualizerState(
    val enabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<EqualizerPreset> = emptyList(),
    val currentPreset: Short = -1, // -1 = custom
)

@Singleton
class AudioEffectsManager @Inject constructor() {

    private var equalizer: Equalizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachToPlayer(exoPlayer: ExoPlayer) {
        release()
        // audioSessionId 在 prepare 后才有效；先监听 ready 再绑定
        val attach = {
            try {
                equalizer = Equalizer(0, exoPlayer.audioSessionId)
                refreshState()
            } catch (_: Exception) {
                // Equalizer not available on this device
            }
        }
        if (exoPlayer.audioSessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
            attach()
        } else {
            exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playbackState == androidx.media3.common.Player.STATE_READY) {
                        exoPlayer.removeListener(this)
                        attach()
                    }
                }
            })
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, level)
            refreshState()
        } catch (_: Exception) {}
    }

    fun usePreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
            _state.value = _state.value.copy(currentPreset = presetIndex)
            refreshState()
        } catch (_: Exception) {}
    }

    private fun refreshState() {
        val eq = equalizer ?: return
        val bands = (0 until eq.numberOfBands.toInt()).map { i ->
            val bandRange = eq.bandLevelRange
            EqualizerBand(
                index = i.toShort(),
                centerFreq = eq.getCenterFreq(i.toShort()),
                minLevel = bandRange[0],
                maxLevel = bandRange[1],
                currentLevel = eq.getBandLevel(i.toShort()),
            )
        }
        val presets = (0 until eq.numberOfPresets.toInt()).map { i ->
            EqualizerPreset(
                index = i.toShort(),
                name = eq.getPresetName(i.toShort()),
            )
        }
        _state.value = _state.value.copy(
            enabled = eq.enabled,
            bands = bands,
            presets = presets,
        )
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
    }
}
