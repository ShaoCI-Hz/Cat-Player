package com.example.smbplayer.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smbplayer.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    var tapCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Title
        Text(
            "设置",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // === 外观 ===
        SectionCard(title = "外观", icon = Icons.Filled.Palette) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeButton("暗色", themeMode == ThemeMode.Dark) { viewModel.setThemeMode(ThemeMode.Dark) }
                ThemeButton("浅色", themeMode == ThemeMode.Light) { viewModel.setThemeMode(ThemeMode.Light) }
                ThemeButton("跟随系统", themeMode == ThemeMode.System) { viewModel.setThemeMode(ThemeMode.System) }
            }
        }

        // === 播放 ===
        SectionCard(title = "播放", icon = Icons.Filled.PlayArrow) {
            SettingRow("默认播放模式", viewModel.playModeLabel) { viewModel.showPlayModePicker = true }
            SettingRow("播放速度", "${viewModel.playbackSpeed}x") { viewModel.showSpeedPicker = true }
            SettingRow("淡入淡出", viewModel.crossfadeLabel, Icons.Filled.SwapHoriz) { viewModel.showCrossfadePicker = true }
        }

        // === 音效 ===
        SectionCard(title = "音效", icon = Icons.Filled.Equalizer) {
            SettingRow("均衡器", "", Icons.Filled.Equalizer) { viewModel.showEqualizer = true }
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.toggleBassBoost(!viewModel.bassBoostOn) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SurroundSound, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("低音增强", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Switch(checked = viewModel.bassBoostOn, onCheckedChange = { viewModel.toggleBassBoost(it) })
            }
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.toggleReplayGain(!viewModel.replayGainEnabled) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.VolumeDown, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("响度标准化", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text("ReplayGain 自动平衡不同歌曲音量", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = viewModel.replayGainEnabled, onCheckedChange = { viewModel.toggleReplayGain(it) })
            }
            // #6: ReplayGain scan button
            SettingRow("扫描音量标签", "扫描本地音乐的ReplayGain标签", Icons.Filled.Analytics) {
                viewModel.scanReplayGain()
            }
            SettingRow("音频输出设备", viewModel.selectedDeviceLabel, Icons.Filled.Speaker) { viewModel.showDevicePicker = true }

            // Channel Balance (A5)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Speaker, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("声道平衡", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(viewModel.channelBalanceLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = viewModel.channelBalance,
                    onValueChange = { viewModel.updateChannelBalance(it) },
                    valueRange = -1f..1f,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("左", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("右", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Pitch Control (A6)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("音调", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(viewModel.pitchLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = viewModel.playbackPitch,
                    onValueChange = { viewModel.setPitch(it) },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0.5x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("1.0x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("2.0x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // === SMB ===
        SectionCard(title = "SMB 网络", icon = Icons.Filled.Cloud) {
            SettingRow("连接状态", viewModel.smbStatus) { viewModel.doDisconnectSmb() }
        }

        // === 本地音乐 ===
        SectionCard(title = "本地音乐", icon = Icons.Filled.Folder) {
            SettingRow("扫描文件夹管理", "", Icons.Filled.FolderOpen) { viewModel.showFolderConfig = true }
            SettingRow("最短歌曲时长", viewModel.minDurationLabel, Icons.Filled.Timer) { viewModel.showDurationPicker = true }
        }

        // === 工具 ===
        SectionCard(title = "工具", icon = Icons.Filled.Build) {
            SettingRow("播放统计", "", Icons.Filled.BarChart) { viewModel.showPlayStats = true }
            SettingRow("睡眠定时器", viewModel.sleepTimerLabel) { viewModel.showSleepTimer = true }
            SettingRow("AB 循环", viewModel.abLoopLabel, Icons.Filled.Pin) { viewModel.doClearABLoop() }
            SettingRow("导出播放列表", ".m3u", Icons.Filled.FileDownload) { viewModel.exportM3u(context) }
            SettingRow("数据备份", "复制", Icons.Filled.Backup) {
                viewModel.exportBackup { json ->
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    android.content.ClipData.newPlainText("backup", json).let { cm.setPrimaryClip(it) }
                }
            }
            SettingRow("数据恢复", "粘贴", Icons.Filled.Restore) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
                if (text != null) viewModel.importBackup(text)
            }
            SettingRow("缓存管理", viewModel.cacheSize, Icons.Filled.CleaningServices) { viewModel.doClearCache() }
        }

        // === 关于 ===
        SectionCard(title = "关于", icon = Icons.Filled.Info) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("版本", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    if (tapCount >= 5) "3.0.0 (开发者)".also { tapCount = 0 } else "3.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tapCount >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { tapCount++; if (tapCount >= 5) tapCount = 5 }
                )
            }
            if (tapCount >= 5) {
                Text(
                    "SDK 36 | Kotlin 2.0.21 | Media3 1.4.1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    // Dialogs
    if (viewModel.showPlayModePicker) PlayModePicker(viewModel)
    if (viewModel.showSpeedPicker) SpeedPicker(viewModel)
    if (viewModel.showSleepTimer) SleepTimerPicker(viewModel)
    if (viewModel.showEqualizer) EqualizerSheet(viewModel)
    if (viewModel.showCrossfadePicker) CrossfadePicker(viewModel)
    if (viewModel.showDevicePicker) DevicePicker(viewModel)
    if (viewModel.showFolderConfig) FolderConfigDialog(viewModel)
    if (viewModel.showDurationPicker) DurationPicker(viewModel)
    if (viewModel.showPlayStats) PlayStatsDialog(viewModel)
}

// ========== Sub-components ==========

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary)
        }
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ThemeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "tb"
    )
    val textColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "tt"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ========== Dialogs ==========

@Composable
private fun PlayModePicker(viewModel: SettingsViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showPlayModePicker = false },
        title = { Text("默认播放模式") },
        text = {
            Column {
                listOf("顺序播放", "随机播放", "单曲循环", "列表循环").forEachIndexed { i, label ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.pickPlayMode(i); viewModel.showPlayModePicker = false }.padding(vertical = 12.dp)) {
                        RadioButton(selected = viewModel.playModeIdx == i, onClick = { viewModel.pickPlayMode(i); viewModel.showPlayModePicker = false })
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SpeedPicker(viewModel: SettingsViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showSpeedPicker = false },
        title = { Text("播放速度") },
        text = {
            Column {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.pickSpeed(s); viewModel.showSpeedPicker = false }.padding(vertical = 12.dp)) {
                        RadioButton(selected = viewModel.playbackSpeed == s, onClick = { viewModel.pickSpeed(s); viewModel.showSpeedPicker = false })
                        Text("${s}x", Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SleepTimerPicker(viewModel: SettingsViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showSleepTimer = false },
        title = { Text("睡眠定时器") },
        text = {
            Column {
                listOf(0 to "关闭", 15 to "15分钟", 30 to "30分钟", 45 to "45分钟", 60 to "60分钟").forEach { (mins, label) ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.pickSleepTimer(mins); viewModel.showSleepTimer = false }.padding(vertical = 12.dp)) {
                        RadioButton(selected = viewModel.sleepTimerMins == mins, onClick = { viewModel.pickSleepTimer(mins); viewModel.showSleepTimer = false })
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerSheet(viewModel: SettingsViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showEqualizer = false },
        title = { Text("均衡器", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                // Presets - horizontal scrollable chips
                Text("预设模式", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.eqPresetCount) { i ->
                        FilterChip(
                            selected = viewModel.eqCurrentPreset == i,
                            onClick = { viewModel.setEqPreset(i) },
                            label = { Text(viewModel.eqPresetName(i), style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                // #5: EQ Visualizer - bar chart showing current levels
                Text("频响曲线", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                    val barCount = viewModel.eqBandCount
                    if (barCount > 0) {
                        val barWidth = size.width / barCount * 0.7f
                        val gap = size.width / barCount * 0.3f
                        val range = viewModel.eqBandRange
                        val midY = size.height / 2

                        for (i in 0 until barCount) {
                            val level = viewModel.eqBandLevel(i).toFloat()
                            val normalizedHeight = (level - range.first()) / (range.last() - range.first()) * size.height * 0.4f
                            val x = i * (barWidth + gap) + gap / 2

                            // Bar
                            drawLine(
                                color = androidx.compose.ui.graphics.Color(0xFF1ED760).copy(alpha = 0.7f),
                                start = androidx.compose.ui.geometry.Offset(x + barWidth / 2, midY),
                                end = androidx.compose.ui.geometry.Offset(x + barWidth / 2, midY - normalizedHeight),
                                strokeWidth = barWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                        // Center line
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(0f, midY),
                            end = androidx.compose.ui.geometry.Offset(size.width, midY),
                            strokeWidth = 1f
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Manual bands
                Text("手动调节", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                repeat(viewModel.eqBandCount) { band ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        val freq = viewModel.eqCenterFreq(band)
                        Text(
                            if (freq >= 1000) "${freq / 1000}K" else "${freq}Hz",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(40.dp)
                        )
                        Slider(
                            value = viewModel.eqBandLevel(band).toFloat(),
                            onValueChange = { viewModel.setEqBand(band, it.toInt()) },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            valueRange = viewModel.eqBandRange.first().toFloat()..viewModel.eqBandRange.last().toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "${viewModel.eqBandLevel(band)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.showEqualizer = false }) { Text("完成") } }
    )
}

@Composable
private fun DevicePicker(viewModel: SettingsViewModel) {
    val devices by viewModel.audioDeviceManager.availableDevices.collectAsState()
    val selectedId by viewModel.audioDeviceManager.selectedDeviceId.collectAsState()
    AlertDialog(
        onDismissRequest = { viewModel.showDevicePicker = false },
        title = { Text("音频输出设备") },
        text = {
            Column {
                // Default option
                Row(Modifier.fillMaxWidth().clickable {
                    viewModel.selectDevice(null)
                    viewModel.showDevicePicker = false
                }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedId == null, onClick = {
                        viewModel.selectDevice(null)
                        viewModel.showDevicePicker = false
                    })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("默认设备", style = MaterialTheme.typography.bodyLarge)
                        Text("系统自动选择", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                // Available devices
                devices.forEach { device ->
                    Row(Modifier.fillMaxWidth().clickable {
                        viewModel.selectDevice(device)
                        viewModel.showDevicePicker = false
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedId == device.id, onClick = {
                            viewModel.selectDevice(device)
                            viewModel.showDevicePicker = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(device.displayName, style = MaterialTheme.typography.bodyLarge)
                            if (device.name.isNotEmpty() && device.name != device.displayName) {
                                Text(device.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (devices.isEmpty()) {
                    Text("未检测到音频输出设备", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.showDevicePicker = false }) { Text("关闭") } }
    )
}

@Composable
private fun FolderConfigDialog(viewModel: SettingsViewModel) {
    val excluded by viewModel.excludedFolders.collectAsState()
    AlertDialog(
        onDismissRequest = { viewModel.showFolderConfig = false },
        title = { Text("扫描文件夹管理") },
        text = {
            Column {
                Text("以下系统文件夹默认不扫描，可手动开启：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                viewModel.defaultExcludedFolders.forEach { folder ->
                    val isExcluded = folder in excluded
                    Row(Modifier.fillMaxWidth().clickable { viewModel.toggleExcludedFolder(folder) }
                        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Folder, null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(folder, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground)
                        Switch(checked = isExcluded, onCheckedChange = { viewModel.toggleExcludedFolder(folder) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.showFolderConfig = false }) { Text("完成") } }
    )
}

@Composable
private fun DurationPicker(viewModel: SettingsViewModel) {
    val durations = listOf(30 to "30秒", 60 to "60秒", 90 to "90秒", 120 to "120秒")
    AlertDialog(
        onDismissRequest = { viewModel.showDurationPicker = false },
        title = { Text("最短歌曲时长") },
        text = {
            Column {
                Text("低于此时间的音频文件将被过滤：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                durations.forEach { (sec, label) ->
                    Row(Modifier.fillMaxWidth().clickable {
                        viewModel.setMinDuration(sec)
                        viewModel.showDurationPicker = false
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = viewModel.minDuration.value == sec, onClick = {
                            viewModel.setMinDuration(sec)
                            viewModel.showDurationPicker = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun PlayStatsDialog(viewModel: SettingsViewModel) {
    LaunchedEffect(Unit) { viewModel.loadPlayStats() }
    val totalPlayTime by viewModel.totalPlayTime.collectAsState()
    val playStats by viewModel.playStats.collectAsState()
    val totalPlays = playStats.values.sum()
    val topTracks = playStats.entries.sortedByDescending { it.value }.take(10)

    AlertDialog(
        onDismissRequest = { viewModel.showPlayStats = false },
        title = { Text("播放统计") },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(Modifier.weight(1f), "总播放次数", "$totalPlays")
                    StatCard(Modifier.weight(1f), "总播放时长", formatDuration(totalPlayTime))
                }
                Spacer(Modifier.height(16.dp))
                if (topTracks.isNotEmpty()) {
                    Text("最常播放 Top 10", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp))
                    topTracks.forEachIndexed { index, (trackId, count) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}.", Modifier.width(24.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text(trackId.substringAfterLast('/').substringBeforeLast('.'),
                                Modifier.weight(1f), maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium)
                            Text("${count}次", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("暂无播放记录", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.showPlayStats = false }) { Text("关闭") } }
    )
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String) {
    Card(modifier, shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}小时${minutes % 60}分"
        minutes > 0 -> "${minutes}分钟"
        else -> "${seconds}秒"
    }
}

@Composable
private fun CrossfadePicker(viewModel: SettingsViewModel) {
    val durations = listOf(0 to "关闭", 1000 to "1秒", 2000 to "2秒", 3000 to "3秒", 5000 to "5秒")
    AlertDialog(
        onDismissRequest = { viewModel.showCrossfadePicker = false },
        title = { Text("淡入淡出") },
        text = {
            Column {
                Text("曲间过渡时长，0为直接切换", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                durations.forEach { (ms, label) ->
                    Row(Modifier.fillMaxWidth().clickable {
                        viewModel.updateCrossfadeDuration(ms)
                        viewModel.showCrossfadePicker = false
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = viewModel.crossfadeDuration == ms, onClick = {
                            viewModel.updateCrossfadeDuration(ms)
                            viewModel.showCrossfadePicker = false
                        })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
