package com.example.smbplayer.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("设置", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

        Column(Modifier.weight(1f)) {

            // ⚙ 播放
            SectionHeader("播放")
            SettingRow("默认播放模式", viewModel.playModeLabel) { viewModel.showPlayModePicker = true }
            SettingRow("播放速度", "${viewModel.playbackSpeed}x") { viewModel.showSpeedPicker = true }

            // 🎵 音效
            SectionHeader("音效")
            SettingRow("均衡器", "", Icons.Filled.Equalizer) { viewModel.showEqualizer = true }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SurroundSound, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("低音增强", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Switch(checked = viewModel.bassBoostOn, onCheckedChange = { viewModel.toggleBassBoost(it) })
            }

            // ☁ 网络
            SectionHeader("网络")
            SettingRow("SMB 连接", viewModel.smbStatus) { viewModel.doDisconnectSmb() }
            SettingRow("离线下载", "管理", Icons.Filled.Download) {}

            // 🔧 工具
            SectionHeader("工具")
            SettingRow("睡眠定时器", viewModel.sleepTimerLabel) { viewModel.showSleepTimer = true }
            SettingRow("AB 循环", viewModel.abLoopLabel, Icons.Filled.Pin) { viewModel.doClearABLoop() }
            SettingRow("导出播放列表 .m3u", "", Icons.Filled.FileDownload) { viewModel.exportM3u(context) }
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

            // 🖥 显示
            SectionHeader("显示")
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = themeMode == ThemeMode.Dark, onClick = { viewModel.setThemeMode(ThemeMode.Dark) })
                Text("暗色", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.width(16.dp))
                RadioButton(selected = themeMode == ThemeMode.System, onClick = { viewModel.setThemeMode(ThemeMode.System) })
                Text("跟随系统", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            }

            // ℹ 关于
            SectionHeader("关于")
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("版本", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    if (tapCount >= 5) "1.0.0 (开发者)".also { tapCount = 0 } else "1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tapCount >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { tapCount++; if (tapCount >= 5) tapCount = 5 }
                )
            }
            if (tapCount >= 5) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("SDK 36 | Kotlin 2.0.21 | Media3 1.4.1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // Dialogs
    if (viewModel.showPlayModePicker) {
        PlayModePicker(viewModel)
    }
    if (viewModel.showSpeedPicker) {
        SpeedPicker(viewModel)
    }
    if (viewModel.showSleepTimer) {
        SleepTimerPicker(viewModel)
    }
    if (viewModel.showEqualizer) {
        EqualizerSheet(viewModel)
    }
}

// ========== Sub-components ==========

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp))
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
                    Row(Modifier.fillMaxWidth().clickable { viewModel.pickPlayMode(i); viewModel.showPlayModePicker = false }.padding(vertical = 8.dp)) {
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
                    Row(Modifier.fillMaxWidth().clickable { viewModel.pickSpeed(s); viewModel.showSpeedPicker = false }.padding(vertical = 8.dp)) {
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
                    Row(Modifier.fillMaxWidth().clickable { viewModel.pickSleepTimer(mins); viewModel.showSleepTimer = false }.padding(vertical = 8.dp)) {
                        RadioButton(selected = viewModel.sleepTimerMins == mins, onClick = { viewModel.pickSleepTimer(mins); viewModel.showSleepTimer = false })
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun EqualizerSheet(viewModel: SettingsViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showEqualizer = false },
        title = { Text("均衡器") },
        text = {
            Column {
                // EQ presets
                Text("预设", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(viewModel.eqPresetCount) { i ->
                        FilterChip(
                            selected = viewModel.eqCurrentPreset == i,
                            onClick = { viewModel.setEqPreset(i) },
                            label = { Text(viewModel.eqPresetName(i), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Band sliders
                Text("手动调节", style = MaterialTheme.typography.titleSmall)
                repeat(viewModel.eqBandCount) { band ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val freq = viewModel.eqCenterFreq(band)
                        Text(if (freq >= 1000) "${freq / 1000}K" else "${freq}Hz",
                            style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
                        Slider(
                            value = viewModel.eqBandLevel(band).toFloat(),
                            onValueChange = { viewModel.setEqBand(band, it.toInt()) },
                            modifier = Modifier.weight(1f),
                            valueRange = viewModel.eqBandRange.first().toFloat()..viewModel.eqBandRange.last().toFloat()
                        )
                        Text("${viewModel.eqBandLevel(band)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.showEqualizer = false }) { Text("关闭") } }
    )
}
