package com.hezi.juyumao.ui.sleep

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SleepTimerViewModel @Inject constructor() : ViewModel() {

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    fun setTimer(minutes: Int) {
        _remainingSeconds.value = minutes * 60
    }

    fun cancelTimer() {
        _remainingSeconds.value = 0
    }
}
