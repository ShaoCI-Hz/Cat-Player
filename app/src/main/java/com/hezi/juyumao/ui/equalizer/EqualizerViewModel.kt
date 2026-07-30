package com.hezi.juyumao.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.player.audio.AudioEffectsManager
import com.hezi.juyumao.player.audio.EqualizerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val audioEffectsManager: AudioEffectsManager,
) : ViewModel() {

    val state: StateFlow<EqualizerState> = audioEffectsManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerState())

    fun setEnabled(enabled: Boolean) {
        audioEffectsManager.setEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        audioEffectsManager.setBandLevel(bandIndex, level)
    }

    fun usePreset(presetIndex: Short) {
        audioEffectsManager.usePreset(presetIndex)
    }
}
