package com.example.smbplayer.ui.connect

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smbplayer.data.smb.ConnectionState

@Composable
fun ConnectScreen(viewModel: ConnectViewModel) {
    val host by viewModel.host.collectAsState()
    val shareName by viewModel.shareName.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SMB 音乐播放器",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "连接网络共享文件夹播放音乐",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = host,
            onValueChange = { viewModel.updateHost(it) },
            label = { Text("服务器地址 (如 192.168.1.100)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = connectionState != ConnectionState.Connecting
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = shareName,
            onValueChange = { viewModel.updateShareName(it) },
            label = { Text("共享名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = connectionState != ConnectionState.Connecting
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { viewModel.updateUsername(it) },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = connectionState != ConnectionState.Connecting
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = connectionState != ConnectionState.Connecting
        )
        Spacer(modifier = Modifier.height(24.dp))

        val error = errorMessage
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (connectionState) {
            ConnectionState.Disconnected, ConnectionState.Error -> {
                Button(
                    onClick = { viewModel.connect() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("连接")
                }
            }
            ConnectionState.Connecting -> {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("连接中...")
                }
            }
            ConnectionState.Connected -> {
                Button(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("断开连接")
                }
            }
        }
    }
}
