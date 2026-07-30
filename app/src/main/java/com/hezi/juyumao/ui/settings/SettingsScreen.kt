package com.hezi.juyumao.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onNavigateToSmb: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Theme section
        item {
            SettingsSection(title = "外观") {
                var themeMode by remember { mutableStateOf("深色") }
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "主题模式",
                    subtitle = themeMode,
                    onClick = { },
                )
            }
        }

        // SMB section
        item {
            SettingsSection(title = "NAS 连接") {
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = "SMB 服务器管理",
                    subtitle = "管理 NAS 连接",
                    onClick = onNavigateToSmb,
                )
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "自动连接",
                    subtitle = "WiFi 下自动连接已保存的 NAS",
                    onClick = { },
                )
            }
        }

        // Audio section
        item {
            SettingsSection(title = "音频") {
                SettingsItem(
                    icon = Icons.Default.Equalizer,
                    title = "均衡器",
                    subtitle = "调节音频效果",
                    onClick = onNavigateToEqualizer,
                )
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "无缝播放",
                    subtitle = "消除曲目切换间隙",
                    onClick = { },
                )
                SettingsItem(
                    icon = Icons.Default.Memory,
                    title = "缓冲大小",
                    subtitle = "256 KB",
                    onClick = { },
                )
            }
        }

        // About section
        item {
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本",
                    subtitle = "1.0.0",
                    onClick = { },
                )
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "开源许可",
                    subtitle = "",
                    onClick = { },
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column { content() }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}
