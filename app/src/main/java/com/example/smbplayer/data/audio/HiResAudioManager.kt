package com.example.smbplayer.data.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Hi-Res audio output configuration.
 * Detects device capabilities and configures optimal output.
 */
@Singleton
class HiResAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AudioOutputInfo(
        val sampleRate: Int,
        val bitDepth: Int,
        val isHiRes: Boolean,
        val outputDevice: String
    )

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Check if the device supports Hi-Res audio output.
     */
    fun isHiResSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ has native Hi-Res audio support
            true
        } else {
            // Check for compatible output devices
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_DEVICE
            }
        }
    }

    /**
     * Get the optimal sample rate for Hi-Res output.
     * Common Hi-Res rates: 44100, 48000, 88200, 96000, 176400, 192000
     */
    fun getOptimalSampleRate(): Int {
        val propertySampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val deviceSampleRate = propertySampleRate?.toIntOrNull() ?: 48000

        // Return the highest supported rate
        return when {
            deviceSampleRate >= 192000 -> 192000
            deviceSampleRate >= 96000 -> 96000
            deviceSampleRate >= 48000 -> 48000
            else -> 44100
        }
    }

    /**
     * Get the optimal buffer size for the given sample rate.
     */
    fun getOptimalBufferSize(sampleRate: Int): Int {
        val propertyBufferSize = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        return propertyBufferSize?.toIntOrNull() ?: (sampleRate / 50) // 20ms buffer
    }

    /**
     * Get current audio output information.
     */
    fun getAudioOutputInfo(): AudioOutputInfo {
        val sampleRate = getOptimalSampleRate()
        val isHiRes = sampleRate >= 96000

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val outputDevice = devices.firstOrNull()?.let { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "有线耳机"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "蓝牙耳机"
                AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB 音频"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "扬声器"
                else -> "未知设备"
            }
        } ?: "未知设备"

        return AudioOutputInfo(
            sampleRate = sampleRate,
            bitDepth = if (isHiRes) 24 else 16,
            isHiRes = isHiRes,
            outputDevice = outputDevice
        )
    }

    /**
     * Format output info for display.
     */
    fun formatOutputInfo(): String {
        val info = getAudioOutputInfo()
        return if (info.isHiRes) {
            "Hi-Res ${info.sampleRate / 1000}kHz/${info.bitDepth}bit → ${info.outputDevice}"
        } else {
            "${info.sampleRate / 1000}kHz/${info.bitDepth}bit → ${info.outputDevice}"
        }
    }
}
