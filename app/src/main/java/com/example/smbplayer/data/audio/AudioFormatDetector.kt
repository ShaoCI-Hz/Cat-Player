package com.example.smbplayer.data.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects audio format information for display in the player UI.
 */
@Singleton
class AudioFormatDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AudioFormatInfo(
        val codec: String = "Unknown",
        val sampleRate: Int = 0,
        val bitDepth: Int = 0,
        val bitrate: Int = 0,
        val channels: Int = 0,
        val isHiRes: Boolean = false,
        val mimeType: String = ""
    ) {
        val sampleRateDisplay: String
            get() = when {
                sampleRate >= 192000 -> "192 kHz"
                sampleRate >= 176400 -> "176.4 kHz"
                sampleRate >= 96000 -> "96 kHz"
                sampleRate >= 88200 -> "88.2 kHz"
                sampleRate >= 48000 -> "48 kHz"
                sampleRate >= 44100 -> "44.1 kHz"
                sampleRate > 0 -> "$sampleRate Hz"
                else -> "Unknown"
            }

        val bitDepthDisplay: String
            get() = if (bitDepth > 0) "${bitDepth}-bit" else ""

        val bitrateDisplay: String
            get() = when {
                bitrate >= 1000000 -> "${"%.1f".format(bitrate / 1000000.0)} Mbps"
                bitrate >= 1000 -> "${bitrate / 1000} kbps"
                bitrate > 0 -> "$bitrate bps"
                else -> ""
            }

        val channelDisplay: String
            get() = when (channels) {
                1 -> "Mono"
                2 -> "Stereo"
                in 3..6 -> "Surround $channels"
                else -> if (channels > 0) "${channels}ch" else ""
            }

        val codecDisplay: String
            get() = when {
                mimeType.contains("flac", true) -> "FLAC"
                mimeType.contains("alac", true) -> "ALAC"
                mimeType.contains("aac", true) -> "AAC"
                mimeType.contains("mp3", true) || mimeType.contains("mpeg", true) -> "MP3"
                mimeType.contains("ogg", true) || mimeType.contains("vorbis", true) -> "OGG"
                mimeType.contains("opus", true) -> "Opus"
                mimeType.contains("wav", true) || mimeType.contains("x-wav", true) -> "WAV"
                mimeType.contains("wma", true) -> "WMA"
                mimeType.contains("ape", true) -> "APE"
                mimeType.contains("dsd", true) || mimeType.contains("dsf", true) -> "DSD"
                codec.isNotEmpty() -> codec
                else -> "Unknown"
            }

        val qualityBadge: String
            get() = when {
                isHiRes -> "Hi-Res"
                sampleRate >= 44100 && bitDepth >= 16 -> "CD Quality"
                bitrate >= 320000 -> "High"
                bitrate >= 128000 -> "Standard"
                else -> ""
            }
    }

    /**
     * Detect audio format from a local URI.
     */
    fun detectFromUri(uri: Uri): AudioFormatInfo {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            if (extractor.trackCount > 0) {
                val format = extractor.getTrackFormat(0)
                val sampleRate = format.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, 0)
                val channels = format.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 0)
                val bitrate = format.getIntegerOrDefault(MediaFormat.KEY_BIT_RATE, 0)
                val mime = format.getStringOrDefault(MediaFormat.KEY_MIME, "")

                // Detect bit depth from PCM encoding if available
                val bitDepth = when {
                    mime.contains("flac", true) -> 24 // FLAC typically 16 or 24
                    mime.contains("wav", true) -> 16
                    sampleRate >= 96000 -> 24 // Hi-Res typically 24-bit
                    else -> 16
                }

                val isHiRes = sampleRate >= 96000 || bitDepth >= 24

                extractor.release()

                AudioFormatInfo(
                    codec = mime.substringAfter("/"),
                    sampleRate = sampleRate,
                    bitDepth = bitDepth,
                    bitrate = bitrate,
                    channels = channels,
                    isHiRes = isHiRes,
                    mimeType = mime
                )
            } else {
                extractor.release()
                AudioFormatInfo()
            }
        } catch (_: Exception) {
            AudioFormatInfo()
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, default: Int): Int {
        return try { if (containsKey(key)) getInteger(key) else default } catch (_: Exception) { default }
    }

    private fun MediaFormat.getStringOrDefault(key: String, default: String): String {
        return try { getString(key) ?: default } catch (_: Exception) { default }
    }
}
