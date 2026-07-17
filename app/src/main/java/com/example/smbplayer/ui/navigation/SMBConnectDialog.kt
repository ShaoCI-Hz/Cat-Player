package com.example.smbplayer.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.smbplayer.data.smb.ConnectionState
import com.example.smbplayer.ui.connect.ConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SMBConnectDialog(
    viewModel: ConnectViewModel,
    onDismiss: () -> Unit
) {
    val host by viewModel.host.collectAsState()
    val shareName by viewModel.shareName.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    if (isConnected) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CloudDone, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("SMB 已连接", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("$host / $shareName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("关闭") }
                    Button(onClick = { viewModel.disconnect(); onDismiss() }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("断开")
                    }
                }
            }
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = { if (connectionState != ConnectionState.Connecting) onDismiss() },
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("连接 SMB 服务器", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))

            SmbField(
                value = host, onValueChange = { viewModel.updateHost(it) },
                label = "服务器地址", icon = Icons.Filled.Dns,
                enabled = connectionState != ConnectionState.Connecting
            )
            SmbField(
                value = shareName, onValueChange = { viewModel.updateShareName(it) },
                label = "共享名称", icon = Icons.Filled.Folder,
                enabled = connectionState != ConnectionState.Connecting
            )
            SmbField(
                value = username, onValueChange = { viewModel.updateUsername(it) },
                label = "用户名", icon = Icons.Filled.Person,
                enabled = connectionState != ConnectionState.Connecting
            )
            SmbField(
                value = password, onValueChange = { viewModel.updatePassword(it) },
                label = "密码", icon = Icons.Filled.Lock,
                enabled = connectionState != ConnectionState.Connecting, isPassword = true
            )

            val err = errorMessage
            if (err != null) {
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(err, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = { viewModel.connect() },
                    modifier = Modifier.weight(1f),
                    enabled = connectionState != ConnectionState.Connecting
                ) {
                    if (connectionState == ConnectionState.Connecting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("连接")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SmbField(
    value: String, onValueChange: (String) -> Unit,
    label: String, icon: ImageVector,
    enabled: Boolean, isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(20.dp)) },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
}
