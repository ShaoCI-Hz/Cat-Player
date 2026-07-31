package com.hezi.juyumao.ui.smb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hezi.juyumao.data.remote.smb.SmbConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbConnectScreen(
    onBack: () -> Unit,
    viewModel: SmbConnectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var ip by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.connectSuccess) {
        if (uiState.connectSuccess) onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Text("NAS 连接", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(48.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // 自动发现
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.WifiFind, null, tint = MaterialTheme.colorScheme.primary)
                            Text("自动发现", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("扫描局域网内的 SMB/NAS 设备", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.discover() }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp), enabled = !uiState.isScanning) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (uiState.isScanning) "扫描中..." else "开始扫描")
                        }
                    }
                }
            }

            // 发现的设备
            if (uiState.discoveredServers.isNotEmpty()) {
                item { Text("发现的设备", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                items(uiState.discoveredServers) { server ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.connectToDiscovered(server) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(server.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("${server.host}:${server.port}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalButton(onClick = { viewModel.connectToDiscovered(server) }) { Text("连接") }
                        }
                    }
                }
            }

            // 已保存的服务器
            if (uiState.savedServers.isNotEmpty()) {
                item { Text("已保存的服务器", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                items(uiState.savedServers) { server ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(server.ip, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("共享: ${server.shareName.ifEmpty { "未设置" }}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalButton(onClick = { viewModel.connectToSaved(server) }) { Text("连接") }
                            IconButton(onClick = { viewModel.deleteServer(server) }) {
                                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // 可用共享列表（连接后发现共享名为空时显示）
            if (uiState.availableShares.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("请选择共享文件夹", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            uiState.availableShares.forEach { share ->
                                OutlinedButton(onClick = {
                                    shareName = share
                                    viewModel.connectWithShare(share)
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(share)
                                }
                            }
                        }
                    }
                }
            }

            // 手动连接
            item { Spacer(Modifier.height(8.dp)); Text("手动连接", style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP 地址") },
                    placeholder = { Text("192.168.1.100") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true)
            }
            item {
                OutlinedTextField(value = shareName, onValueChange = { shareName = it }, label = { Text("共享名称") },
                    placeholder = { Text("music / Media / 共享文件夹名") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true)
            }
            item {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") },
                    placeholder = { Text("留空则匿名连接") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true)
            }
            item {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true,
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None
                    else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    })
            }

            // 连接按钮
            item {
                Button(
                    onClick = { viewModel.connect(ip, 445, username, password, shareName) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = ip.isNotEmpty() && uiState.connectionState !is SmbConnectionState.Connecting,
                ) {
                    if (uiState.connectionState is SmbConnectionState.Connecting) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(when (uiState.connectionState) {
                        is SmbConnectionState.Connecting -> "连接中..."
                        is SmbConnectionState.Connected -> "已连接"
                        else -> "连接"
                    })
                }
            }

            // 错误信息
            uiState.errorMessage?.let { error ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(error, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 使用说明
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("连接说明", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("1. 确保手机和 NAS 在同一 WiFi 网络", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2. 输入 NAS 的 IP 地址（如 192.168.1.100）", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("3. 输入共享名称（如 music、Media）", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("4. 用户名留空则匿名连接", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("5. 连接成功后可在「浏览」页面看到 NAS 音乐", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
