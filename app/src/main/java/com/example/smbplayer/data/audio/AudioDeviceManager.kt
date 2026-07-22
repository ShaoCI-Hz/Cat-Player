package com.example.smbplayer.data.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AudioDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val isOutput: Boolean
) {
    val displayName: String
        get() = when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "扬声器"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "有线耳机"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "有线耳机"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "蓝牙耳机"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "蓝牙通话"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB 耳机"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB 设备"
            else -> name.ifEmpty { "音频设备 #$id" }
        }

    val icon: String
        get() = when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "headset"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "bluetooth"
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "usb"
            else -> "device"
        }
}

@Singleton
class AudioDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<Int?>(null)
    val selectedDeviceId: StateFlow<Int?> = _selectedDeviceId.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refreshDevices()
            // If selected device was removed, reset selection
            val selectedId = _selectedDeviceId.value
            if (selectedId != null && removedDevices.any { it.id == selectedId }) {
                _selectedDeviceId.value = null
            }
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        refreshDevices()
    }

    fun refreshDevices() {
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        _availableDevices.value = outputDevices.map { info ->
            AudioDevice(
                id = info.id,
                name = info.productName?.toString() ?: "",
                type = info.type,
                isOutput = true
            )
        }
    }

    /**
     * Set the preferred audio output device.
     * Pass null to reset to default routing.
     */
    fun setPreferredDevice(device: AudioDevice?) {
        if (device == null) {
            _selectedDeviceId.value = null
            // ExoPlayer doesn't have setPreferredAudioDevice, use AudioManager
            return
        }
        _selectedDeviceId.value = device.id
    }

    /**
     * Get the AudioDeviceInfo for the currently selected device.
     * Returns null if no device is selected or device not found.
     */
    fun getSelectedDeviceInfo(): AudioDeviceInfo? {
        val selectedId = _selectedDeviceId.value ?: return null
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .find { it.id == selectedId }
    }

    fun release() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }
}
