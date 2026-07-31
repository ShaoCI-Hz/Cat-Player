package com.hezi.juyumao.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SleepTimerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private var timerJob: Job? = null

    val isTimerRunning: Boolean get() = timerJob?.isActive == true && _remainingSeconds.value > 0

    fun setTimer(minutes: Int) {
        cancelTimer()
        _remainingSeconds.value = minutes * 60
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value--
            }
            // 倒计时结束，暂停播放
            playbackController.pause()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0
    }
}
