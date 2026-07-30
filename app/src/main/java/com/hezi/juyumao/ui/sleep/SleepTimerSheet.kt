package com.hezi.juyumao.ui.sleep

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class TimerOption(val label: String, val minutes: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    onDismiss: () -> Unit,
    onTimerSet: (Int) -> Unit,
    onTimerCancel: () -> Unit,
    remainingSeconds: Int = 0,
) {
    val options = listOf(
        TimerOption("15 分钟", 15),
        TimerOption("30 分钟", 30),
        TimerOption("45 分钟", 45),
        TimerOption("60 分钟", 60),
        TimerOption("90 分钟", 90),
    )

    var isTimerRunning by remember { mutableStateOf(remainingSeconds > 0) }
    var remaining by remember { mutableIntStateOf(remainingSeconds) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && remaining > 0) {
            delay(1000)
            remaining--
            if (remaining <= 0) {
                isTimerRunning = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "定时关闭",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isTimerRunning) {
                // Countdown display
                val minutes = remaining / 60
                val seconds = remaining % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isTimerRunning = false
                        remaining = 0
                        onTimerCancel()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("取消定时")
                }
            } else {
                // Options
                options.forEach { option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                remaining = option.minutes * 60
                                isTimerRunning = true
                                onTimerSet(option.minutes)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
