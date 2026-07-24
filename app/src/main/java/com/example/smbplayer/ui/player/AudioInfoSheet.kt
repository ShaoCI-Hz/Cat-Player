package com.example.smbplayer.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smbplayer.data.audio.AudioFormatDetector
import com.example.smbplayer.ui.theme.CatPlayerHiResGold

/**
 * Audio format information sheet showing codec, bitrate, sample rate, etc.
 */
@Composable
fun AudioInfoSheet(
    formatInfo: AudioFormatDetector.AudioFormatInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("音频信息", style = MaterialTheme.typography.headlineSmall)
                if (formatInfo.isHiRes) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CatPlayerHiResGold
                    ) {
                        Text(
                            "Hi-Res",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quality badge
                if (formatInfo.qualityBadge.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (formatInfo.isHiRes) CatPlayerHiResGold.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (formatInfo.isHiRes) Icons.Filled.Star else Icons.Filled.MusicNote,
                                null,
                                tint = if (formatInfo.isHiRes) CatPlayerHiResGold else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    formatInfo.qualityBadge,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (formatInfo.isHiRes) CatPlayerHiResGold else MaterialTheme.colorScheme.primary
                                )
                                if (formatInfo.isHiRes) {
                                    Text(
                                        "高解析度音频",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Format details
                Card(shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioInfoRow("编码格式", formatInfo.codecDisplay)
                        if (formatInfo.sampleRate > 0) {
                            AudioInfoRow("采样率", formatInfo.sampleRateDisplay)
                        }
                        if (formatInfo.bitDepth > 0) {
                            AudioInfoRow("位深", formatInfo.bitDepthDisplay)
                        }
                        if (formatInfo.bitrate > 0) {
                            AudioInfoRow("比特率", formatInfo.bitrateDisplay)
                        }
                        if (formatInfo.channels > 0) {
                            AudioInfoRow("声道", formatInfo.channelDisplay)
                        }
                        if (formatInfo.mimeType.isNotEmpty()) {
                            AudioInfoRow("MIME", formatInfo.mimeType)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun AudioInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
    }
}
