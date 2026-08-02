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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbConnectScreen(
    onBack: () -> Unit,
    viewModel: SmbConnectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("445") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.connectSuccess, uiState.isScanningMusic) {
        if (uiState.connectSuccess && !uiState.isScanningMusic) onBack()
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

            // ═══ 自动发现 ═══
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.discover() }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp), enabled = !uiState.isScanning) {
                                Text("mDNS 发现")
                            }
                            Button(onClick = { viewModel.scanNetwork() }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp), enabled = !uiState.isScanning) {
                                if (uiState.isScanning) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${uiState.scanProgress}/254")
                                } else {
                                    Text("扫描网段")
                                }
                            }
                        }
                    }
                }
            }

            // ═══ 扫描到的 SMB 服务器 ═══
            if (uiState.scannedHosts.isNotEmpty()) {
                item {
                    Text("扫描到的 SMB 服务器", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                items(uiState.scannedHosts) { host ->
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        ip = host.ip; port = "445"
                    },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(host.hostname, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(host.ip, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalButton(onClick = { ip = host.ip; port = "445" }) { Text("选用") }
                        }
                    }
                }
            }

            // ═══ mDNS 发现的设备 ═══
            if (uiState.discoveredServers.isNotEmpty()) {
                item { Text("mDNS 发现的设备", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                items(uiState.discoveredServers) { server ->
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        ip = server.host; port = server.port.toString()
                    },
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
                            FilledTonalButton(onClick = { ip = server.host; port = server.port.toString() }) { Text("选用") }
                        }
                    }
                }
            }

            // ═══ 已保存的服务器 ═══
            if (uiState.savedServers.isNotEmpty()) {
                item { Text("已保存的服务器", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                items(uiState.savedServers) { server ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${server.ip}:${server.port}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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

            // ═══ 可用共享列表 ═══
            if (uiState.isDiscoveringShares) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("正在搜索可用共享...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (uiState.availableShares.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("发现以下可用共享，请选择：", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            uiState.availableShares.forEach { share ->
                                OutlinedButton(onClick = {
                                    shareName = share.name
                                    viewModel.connect(
                                        uiState.currentServerIp, uiState.currentServerPort,
                                        username, password, share.name, domain
                                    )
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(share.name)
                                }
                            }
                        }
                    }
                }
            }
            if (!uiState.isDiscoveringShares && uiState.availableShares.isEmpty() && uiState.errorMessage != null && uiState.connectionState !is SmbConnectionState.Error) {
                // 搜索共享失败，显示帮助信息
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("自动搜索未找到共享", style = MaterialTheme.typography.titleSmall)
                            Text("请手动输入共享名。查找方法：", style = MaterialTheme.typography.bodySmall)
                            Text("• 电脑打开 \\\\NAS的IP 查看共享文件夹", style = MaterialTheme.typography.bodySmall)
                            Text("• 或在 NAS 管理页面查看「文件共享」设置", style = MaterialTheme.typography.bodySmall)
                            Text("• 小米 NAS 通常共享名格式如「我的文档-xxxxx」", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ═══ 手动连接 ═══
            item { Spacer(Modifier.height(8.dp)); Text("手动连接", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP 地址") },
                        placeholder = { Text("192.168.1.100") }, modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(8.dp), singleLine = true)
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") },
                        placeholder = { Text("445") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp), singleLine = true)
                }
            }
            item {
                OutlinedTextField(value = shareName, onValueChange = { shareName = it },
                    label = { Text("共享名 / 完整路径") },
                    placeholder = { Text("如 我的文档-xxx 或 我的文档-xxx/Music") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (ip.isNotEmpty()) {
                                viewModel.discoverShares(ip, port.toIntOrNull() ?: 445, username, password, domain)
                            }
                        }, enabled = ip.isNotEmpty()) {
                            Icon(Icons.Default.Search, "搜索共享")
                        }
                    })
            }
            item {
                Text("支持格式：共享名 或 共享名/子目录（自动扫描指定目录下的音乐）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") },
                    placeholder = { Text("留空或填 nobody 为匿名连接") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { username = "nobody"; password = "" }) {
                            Text("匿名", style = MaterialTheme.typography.labelSmall)
                        }
                    })
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

            // ═══ 高级选项 ═══
            item {
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (showAdvanced) "隐藏高级选项" else "高级选项（域、工作组）")
                }
            }
            if (showAdvanced) {
                item {
                    OutlinedTextField(value = domain, onValueChange = { domain = it }, label = { Text("域/工作组") },
                        placeholder = { Text("如 WORKGROUP（通常留空）") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp), singleLine = true)
                }
            }

            // ═══ 操作按钮 ═══
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.discoverShares(ip, port.toIntOrNull() ?: 445, username, password, domain) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = ip.isNotEmpty() && !uiState.isDiscoveringShares,
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("搜索共享")
                    }
                    Button(
                        onClick = {
                            if (shareName.isNotEmpty()) {
                                viewModel.connect(ip, port.toIntOrNull() ?: 445, username, password, shareName, domain)
                            } else {
                                // 共享名为空时，自动搜索可用共享
                                viewModel.discoverShares(ip, port.toIntOrNull() ?: 445, username, password, domain)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = ip.isNotEmpty() && uiState.connectionState !is SmbConnectionState.Connecting,
                    ) {
                        if (uiState.connectionState is SmbConnectionState.Connecting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(when {
                            uiState.connectionState is SmbConnectionState.Connecting -> "连接中..."
                            uiState.isScanningMusic -> "扫描中..."
                            uiState.connectionState is SmbConnectionState.Connected -> "已连接"
                            shareName.isEmpty() -> "搜索共享"
                            else -> "连接"
                        })
                    }
                }
            }

            // ═══ 错误信息 ═══
            uiState.errorMessage?.let { error ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Text("连接失败", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(error, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(8.dp))
                            Text("排查建议：", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• 确认手机和 NAS 在同一 WiFi", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• 检查 IP 和端口是否正确", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• 检查用户名密码是否正确", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• 有些 NAS 需要指定域（如 WORKGROUP）", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• NAS 需要支持 SMB2 或 SMB3 协议", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { viewModel.clearError() }) { Text("关闭") }
                        }
                    }
                }
            }

            // ═══ 连接状态 ═══
            if (uiState.isConnected) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("已连接: ${uiState.connectedServerName}", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("连接将保持 30 分钟，可返回主页浏览音乐", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // ═══ 扫描进度 ═══
            if (uiState.isScanningMusic) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("正在扫描 NAS 音乐...", style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("首次扫描可能需要几分钟，取决于音乐数量", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                            Text("扫描期间可以返回主页，不会中断", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            if (uiState.scanMusicMessage.isNotEmpty() && !uiState.isScanningMusic) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor =
                        if (uiState.scannedSongCount > 0) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                if (uiState.scannedSongCount > 0) Icons.Default.CheckCircle else Icons.Default.Info,
                                null, tint = if (uiState.scannedSongCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(uiState.scanMusicMessage, style = MaterialTheme.typography.bodyMedium)
                                if (uiState.scannedSongCount > 0) {
                                    Text("返回主页即可浏览和播放", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // ═══ 使用说明 ═══
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("连接步骤", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("1. 点击「扫描网段」找到 NAS 的 IP", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2. 点击「选用」填入 IP", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("3. 输入用户名和密码（留空则匿名连接）", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("4. 输入共享名，如「我的文档-xxx」", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("   可加子目录：「我的文档-xxx/Music」只扫描该目录", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("5. 连接后自动扫描音乐，可返回主页浏览", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
