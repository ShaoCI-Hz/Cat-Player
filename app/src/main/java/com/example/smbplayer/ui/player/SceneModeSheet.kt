package com.example.smbplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smbplayer.smart.SceneModeManager

/**
 * Scene mode selection sheet for different listening scenarios.
 */
@Composable
fun SceneModeSheet(
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(SceneModeManager.SceneMode.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("场景模式", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "选择适合当前场景的播放模式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                SceneModeManager.SceneMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { selectedMode = mode }
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                getSceneModeIcon(mode),
                                null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    mode.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

private fun getSceneModeIcon(mode: SceneModeManager.SceneMode): ImageVector {
    return when (mode) {
        SceneModeManager.SceneMode.NORMAL -> Icons.Filled.MusicNote
        SceneModeManager.SceneMode.DRIVING -> Icons.Filled.DirectionsCar
        SceneModeManager.SceneMode.SLEEP -> Icons.Filled.Bedtime
        SceneModeManager.SceneMode.FOCUS -> Icons.Filled.Psychology
        SceneModeManager.SceneMode.PARTY -> Icons.Filled.Celebration
    }
}
