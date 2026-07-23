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
import com.example.smbplayer.data.audio.AudioDeviceManager
import com.example.smbplayer.data.audio.AudioDevice
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
    private val connectionManager: SmbConnectionManager,
    val audioDeviceManager: AudioDeviceManager
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

    // Channel balance (A5): -1.0=left, 0.0=center, 1.0=right
    var channelBalance by mutableStateOf(0f)
    val channelBalanceLabel get() = when {
        channelBalance < -0.1f -> "偏左 ${(channelBalance * -100).toInt()}%"
        channelBalance > 0.1f -> "偏右 ${(channelBalance * 100).toInt()}%"
        else -> "居中"
    }
    fun updateChannelBalance(balance: Float) {
        channelBalance = balance
        audioEffectManager.setChannelBalance(balance)
    }

    // Pitch control (A6)
    var playbackPitch by mutableStateOf(1.0f)
    val pitchLabel get() = "${playbackPitch}x"
    fun setPitch(pitch: Float) {
        playbackPitch = pitch
        playerRepository.setPitch(pitch)
    }

    // N1: Crossfade settings
    var crossfadeDuration by mutableStateOf(0)
    val crossfadeLabel get() = if (crossfadeDuration > 0) "${crossfadeDuration / 1000}秒" else "关闭"
    fun updateCrossfadeDuration(ms: Int) {
        crossfadeDuration = ms
        playerRepository.crossfadeManager.crossfadeDurationMs = ms.toLong()
    }

    var sleepTimerMins by mutableIntStateOf(0)
    val sleepTimerLabel get() = if (sleepTimerMins > 0) "${sleepTimerMins}分钟" else "关闭"
    val abLoopLabel get() = "已关闭"
    val smbStatus get() = if (connectionManager.connectionState.value == ConnectionState.Connected) "已连接" else "未连接"
    val cacheSize get() = "0 KB"

    var showPlayModePicker by mutableStateOf(false)
    var showSpeedPicker by mutableStateOf(false)
    var showSleepTimer by mutableStateOf(false)
    var showEqualizer by mutableStateOf(false)
    var showDevicePicker by mutableStateOf(false)
    var showPlayStats by mutableStateOf(false)
    var showCrossfadePicker by mutableStateOf(false)

    // ReplayGain
    var replayGainEnabled by mutableStateOf(true)
    val replayGainLabel get() = if (replayGainEnabled) "已开启" else "已关闭"
    fun toggleReplayGain(on: Boolean) {
        replayGainEnabled = on
        playerRepository.replayGainProcessor.isEnabled = on
    }

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { saved ->
                _themeMode.value = when (saved) { "dark" -> ThemeMode.Dark; "light" -> ThemeMode.Light; else -> ThemeMode.System }
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
        viewModelScope.launch { settingsRepository.setThemeMode(when (mode) { ThemeMode.Dark -> "dark"; ThemeMode.Light -> "light"; ThemeMode.System -> "system" }) }
    }
    fun pickPlayMode(i: Int) { playModeIdx = i; viewModelScope.launch { settingsRepository.setPlayMode(when(i){0->PlayMode.Sequential;1->PlayMode.Random;2->PlayMode.Single;3->PlayMode.Loop;else->PlayMode.Sequential}) } }
    fun pickSpeed(s: Float) { playbackSpeed = s; playerRepository.setSpeed(s) }
    fun pickSleepTimer(mins: Int) { sleepTimerMins = mins; playerRepository.setSleepTimer(mins) }
    fun doDisconnectSmb() { viewModelScope.launch { connectionManager.disconnect() } }
    fun doClearCache() { /* Coil image cache clear TBD */ }
    fun doClearABLoop() { playerRepository.clearABLoop() }

    // Audio device routing
    val selectedDevice get() = audioDeviceManager.selectedDeviceId.value?.let { id ->
        audioDeviceManager.availableDevices.value.find { it.id == id }
    }
    val selectedDeviceLabel get() = selectedDevice?.displayName ?: "默认设备"
    fun selectDevice(device: AudioDevice?) {
        audioDeviceManager.setPreferredDevice(device)
    }

    // Folder scan configuration
    var showFolderConfig by mutableStateOf(false)
    var showDurationPicker by mutableStateOf(false)
    private val _excludedFolders = MutableStateFlow<Set<String>>(emptySet())
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()
    private val _minDuration = MutableStateFlow(60)
    val minDuration: StateFlow<Int> = _minDuration.asStateFlow()
    val minDurationLabel get() = "${_minDuration.value}秒"

    init {
        viewModelScope.launch {
            settingsRepository.excludedFolders.collect { _excludedFolders.value = it }
        }
        viewModelScope.launch {
            settingsRepository.minDurationSeconds.collect { _minDuration.value = it }
        }
    }

    fun toggleExcludedFolder(folder: String) {
        val current = _excludedFolders.value.toMutableSet()
        if (folder in current) current.remove(folder) else current.add(folder)
        _excludedFolders.value = current
        viewModelScope.launch { settingsRepository.setExcludedFolders(current) }
    }

    fun setMinDuration(seconds: Int) {
        _minDuration.value = seconds
        viewModelScope.launch { settingsRepository.setMinDurationSeconds(seconds) }
    }

    // Default excluded folders for display
    val defaultExcludedFolders = listOf("Ringtones", "Notifications", "Alarms", "Podcasts", "Audiobooks", "Recordings")

    // Play stats
    private val _totalPlayTime = MutableStateFlow(0L)
    val totalPlayTime: StateFlow<Long> = _totalPlayTime.asStateFlow()
    private val _playStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playStats: StateFlow<Map<String, Int>> = _playStats.asStateFlow()

    fun loadPlayStats() {
        viewModelScope.launch {
            _totalPlayTime.value = settingsRepository.totalPlayTimeMs.first()
            _playStats.value = settingsRepository.getPlayStats()
        }
    }

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
