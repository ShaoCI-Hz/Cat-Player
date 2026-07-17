package com.example.smbplayer.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smbplayer.data.audio.AudioEffectManager
import com.example.smbplayer.data.player.PlayMode
import com.example.smbplayer.data.player.PlayerRepository
import com.example.smbplayer.data.settings.SettingsRepository
import com.example.smbplayer.data.smb.ConnectionState
import com.example.smbplayer.data.smb.SmbConnectionManager
import com.example.smbplayer.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val audioEffectManager: AudioEffectManager,
    private val playerRepository: PlayerRepository,
    private val connectionManager: SmbConnectionManager
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.System)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    var playModeIdx by mutableIntStateOf(0)
    var playbackSpeed by mutableStateOf(1.0f)
    val playModeLabel get() = listOf("顺序", "随机", "单曲", "循环")[playModeIdx]

    var bassBoostOn by mutableStateOf(false)
    val eqBandCount get() = audioEffectManager.numberOfBands.toInt()
    val eqBandRange get() = audioEffectManager.getBandLevelRange().toList()
    val eqCurrentPreset get() = audioEffectManager.getCurrentPreset().toInt()
    val eqPresetCount get() = audioEffectManager.getNumberOfPresets().toInt()
    fun eqPresetName(i: Int) = audioEffectManager.getPresetName(i.toShort())
    fun eqCenterFreq(band: Int) = audioEffectManager.getCenterFreq(band.toShort())
    fun eqBandLevel(band: Int) = audioEffectManager.getBandLevel(band.toShort())
    fun setEqBand(band: Int, level: Int) { audioEffectManager.setBandLevel(band.toShort(), level) }
    fun setEqPreset(i: Int) { audioEffectManager.usePreset(i.toShort()) }
    fun toggleBassBoost(on: Boolean) { bassBoostOn = on; audioEffectManager.setBassBoostEnabled(on) }

    var sleepTimerMins by mutableIntStateOf(0)
    val sleepTimerLabel get() = if (sleepTimerMins > 0) "${sleepTimerMins}分钟" else "关闭"
    val abLoopLabel get() = "已关闭"
    val smbStatus get() = if (connectionManager.connectionState.value == ConnectionState.Connected) "已连接" else "未连接"
    val cacheSize get() = "0 KB"

    var showPlayModePicker by mutableStateOf(false)
    var showSpeedPicker by mutableStateOf(false)
    var showSleepTimer by mutableStateOf(false)
    var showEqualizer by mutableStateOf(false)

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { saved ->
                _themeMode.value = when (saved) { "dark" -> ThemeMode.Dark; else -> ThemeMode.System }
            }
        }
        viewModelScope.launch {
            settingsRepository.playMode.first().let { mode ->
                playModeIdx = when (mode) { PlayMode.Sequential -> 0; PlayMode.Random -> 1; PlayMode.Single -> 2; PlayMode.Loop -> 3; }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch { settingsRepository.setThemeMode(when (mode) { ThemeMode.Dark -> "dark"; ThemeMode.System -> "system" }) }
    }
    fun pickPlayMode(i: Int) { playModeIdx = i; viewModelScope.launch { settingsRepository.setPlayMode(when(i){0->PlayMode.Sequential;1->PlayMode.Random;2->PlayMode.Single;3->PlayMode.Loop;else->PlayMode.Sequential}) } }
    fun pickSpeed(s: Float) { playbackSpeed = s; playerRepository.setSpeed(s) }
    fun pickSleepTimer(mins: Int) { sleepTimerMins = mins; playerRepository.setSleepTimer(mins) }
    fun doDisconnectSmb() { viewModelScope.launch { connectionManager.disconnect() } }
    fun doClearCache() { /* Coil image cache clear TBD */ }
    fun doClearABLoop() { playerRepository.clearABLoop() }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val theme = settingsRepository.themeMode.first()
            onResult("""{"theme":"$theme","version":1}""")
        }
    }
    fun importBackup(json: String) {
        viewModelScope.launch {
            try { val t = Regex("\"theme\":\"([^\"]*)\"").find(json)?.groupValues?.get(1) ?: "system"; settingsRepository.setThemeMode(t) } catch (_: Exception) {}
        }
    }
    fun exportM3u(context: Context) {
        viewModelScope.launch {
            val playlist = settingsRepository.savedPlaylist.first()
            val tracks = settingsRepository.parsePlaylist(playlist)
            val content = buildString {
                append("#EXTM3U\n")
                tracks.forEach { t -> append("#EXTINF:${t.durationMs/1000},${t.artist} - ${t.title}\n${t.smbPath.ifEmpty { t.localUri ?: "" }}\n") }
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/x-mpegurl"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(intent, "导出"))
        }
    }
}
