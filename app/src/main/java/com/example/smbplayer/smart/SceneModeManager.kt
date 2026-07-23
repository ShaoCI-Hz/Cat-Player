package com.example.smbplayer.smart

import com.example.smbplayer.data.audio.AudioEffectManager
import com.example.smbplayer.data.audio.CrossfadeManager
import com.example.smbplayer.data.player.PlayMode
import com.example.smbplayer.data.player.PlayerRepository
import com.example.smbplayer.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scene modes that batch-apply settings for different listening scenarios.
 */
@Singleton
class SceneModeManager @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val audioEffectManager: AudioEffectManager,
    private val crossfadeManager: CrossfadeManager,
    private val settingsRepository: SettingsRepository
) {
    enum class SceneMode(val displayName: String, val description: String) {
        NORMAL("普通", "默认设置"),
        DRIVING("驾驶", "大控件，简化操作"),
        SLEEP("睡眠", "定时器+淡出+降低低音"),
        FOCUS("专注", "无通知，稳定音量"),
        PARTY("派对", "随机播放，增强低音高音")
    }

    private var currentMode = SceneMode.NORMAL
    private var savedSettings: SavedSettings? = null

    data class SavedSettings(
        val playMode: PlayMode = PlayMode.Sequential,
        val crossfadeMs: Long = 0,
        val bassBoostOn: Boolean = false,
        val volume: Float = 0.8f,
        val speed: Float = 1.0f
    )

    fun getCurrentMode(): SceneMode = currentMode

    /**
     * Apply a scene mode, saving current settings first.
     */
    fun applyMode(mode: SceneMode) {
        if (currentMode == mode) return

        // Save current settings before switching
        if (savedSettings == null) {
            savedSettings = SavedSettings(
                crossfadeMs = crossfadeManager.crossfadeDurationMs,
                bassBoostOn = audioEffectManager.isBassBoostEnabled,
                volume = 0.8f,
                speed = playerRepository.getSpeed()
            )
        }

        currentMode = mode

        when (mode) {
            SceneMode.NORMAL -> restoreSettings()
            SceneMode.DRIVING -> applyDrivingMode()
            SceneMode.SLEEP -> applySleepMode()
            SceneMode.FOCUS -> applyFocusMode()
            SceneMode.PARTY -> applyPartyMode()
        }
    }

    /**
     * Restore original settings.
     */
    private fun restoreSettings() {
        val saved = savedSettings ?: return
        crossfadeManager.crossfadeDurationMs = saved.crossfadeMs
        audioEffectManager.setBassBoostEnabled(saved.bassBoostOn)
        playerRepository.setSpeed(saved.speed)
        savedSettings = null
    }

    private fun applyDrivingMode() {
        // Simplified controls, moderate volume
        crossfadeManager.crossfadeDurationMs = 2000
        audioEffectManager.setBassBoostEnabled(false)
        playerRepository.setSpeed(1.0f)
    }

    private fun applySleepMode() {
        // Reduced bass, no crossfade, slower speed
        crossfadeManager.crossfadeDurationMs = 0
        audioEffectManager.setBassBoostEnabled(false)
        playerRepository.setSpeed(0.9f)
    }

    private fun applyFocusMode() {
        // Stable volume, no crossfade
        crossfadeManager.crossfadeDurationMs = 0
        audioEffectManager.setBassBoostEnabled(false)
        playerRepository.setSpeed(1.0f)
    }

    private fun applyPartyMode() {
        // Shuffle, bass boost, max crossfade
        crossfadeManager.crossfadeDurationMs = 3000
        audioEffectManager.setBassBoostEnabled(true)
        audioEffectManager.setBassBoostStrength(1000)
        playerRepository.setSpeed(1.0f)
    }
}
