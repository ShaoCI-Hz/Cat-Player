package com.example.smbplayer.domain

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Reads ReplayGain tags from audio files and applies gain correction.
 * ReplayGain tags are stored as TXXX frames in ID3v2 tags.
 * Supports: REPLAYGAIN_TRACK_GAIN, REPLAYGAIN_ALBUM_GAIN, REPLAYGAIN_TRACK_PEAK, REPLAYGAIN_ALBUM_PEAK
 */
@Singleton
class ReplayGainProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Pre-amp gain to avoid clipping (default: -6 dB)
    private var preAmpGain = -6.0f

    // Whether ReplayGain processing is enabled
    var isEnabled = true

    /**
     * Extract ReplayGain info from a local audio file.
     * Reads TXXX frames for ReplayGain tags.
     */
    suspend fun extractTrackGain(localUri: String): ReplayGainInfo? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(localUri)
            val path = getPathFromUri(uri) ?: return@withContext null
            val file = File(path)
            if (!file.exists()) return@withContext null

            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: return@withContext null

            // Read ReplayGain tags - works for both ID3v2 TXXX and Vorbis comments
            val trackGain = tag.getFirst("REPLAYGAIN_TRACK_GAIN")
                .ifEmpty { tag.getFirst("replaygain_track_gain") }
                .replace(" dB", "").replace("dB", "").trim().toFloatOrNull()
            val albumGain = tag.getFirst("REPLAYGAIN_ALBUM_GAIN")
                .ifEmpty { tag.getFirst("replaygain_album_gain") }
                .replace(" dB", "").replace("dB", "").trim().toFloatOrNull()
            val trackPeak = tag.getFirst("REPLAYGAIN_TRACK_PEAK")
                .ifEmpty { tag.getFirst("replaygain_track_peak") }
                .trim().toFloatOrNull()
            val albumPeak = tag.getFirst("REPLAYGAIN_ALBUM_PEAK")
                .ifEmpty { tag.getFirst("replaygain_album_peak") }
                .trim().toFloatOrNull()

            if (trackGain == null && albumGain == null) return@withContext null

            ReplayGainInfo(
                trackGain = trackGain,
                albumGain = albumGain,
                trackPeak = trackPeak,
                albumPeak = albumPeak
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculate the linear gain multiplier from a ReplayGain dB value.
     * Formula: linearGain = 10^((gain_dB + preAmp) / 20)
     * Clamped to [0.0, 1.0] to prevent clipping.
     */
    fun calculateLinearGain(gainDb: Float, peak: Float? = null): Float {
        val adjustedGain = gainDb + preAmpGain
        var linearGain = 10.0f.pow(adjustedGain / 20.0f)

        // Prevent clipping: if peak is available, ensure gain doesn't push signal above 1.0
        if (peak != null && peak > 0f) {
            val maxGain = 1.0f / peak
            linearGain = linearGain.coerceAtMost(maxGain)
        }

        return linearGain.coerceIn(0.0f, 1.0f)
    }

    /**
     * Get the effective gain for a track, preferring track gain over album gain.
     * Returns null if ReplayGain is not available or disabled.
     */
    fun getEffectiveGain(info: ReplayGainInfo?): Float? {
        if (!isEnabled || info == null) return null
        val gainDb = info.trackGain ?: info.albumGain ?: return null
        val peak = info.trackPeak ?: info.albumPeak
        return calculateLinearGain(gainDb, peak)
    }

    private fun getPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Audio.Media.DATA), null, null, null)
            return cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
        }
        return uri.path
    }
}

data class ReplayGainInfo(
    val trackGain: Float?,
    val albumGain: Float?,
    val trackPeak: Float?,
    val albumPeak: Float?
)
