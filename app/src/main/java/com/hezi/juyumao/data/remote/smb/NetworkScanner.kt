package com.hezi.juyumao.data.remote.smb

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import javax.inject.Inject

/**
 * 局域网 SMB 服务器扫描器
 * 通过扫描本地网段的 445 端口来发现 SMB 服务器
 */
class NetworkScanner @Inject constructor() {

    data class ScannedHost(
        val ip: String,
        val hostname: String = "",
        val hasSmb: Boolean = false,
    )

    /**
     * 获取本机 IP 和子网
     */
    private fun getLocalNetwork(): Pair<String, Int>? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress ?: continue
                        val prefixLength = networkInterface.interfaceAddresses
                            .firstOrNull { it.address == address }
                            ?.networkPrefixLength ?: 24
                        return Pair(ip, prefixLength.toInt())
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * 扫描局域网内开放 445 端口的设备
     * @param onProgress 进度回调 (已扫描IP, 发现的SMB服务器列表)
     * @param timeoutMs 每个 IP 的连接超时
     */
    suspend fun scanLocalNetwork(
        onProgress: (Int, List<ScannedHost>) -> Unit = { _, _ -> },
        timeoutMs: Int = 200,
    ): List<ScannedHost> = withContext(Dispatchers.IO) {
        val (localIp, prefixLength) = getLocalNetwork() ?: return@withContext emptyList()
        val ipParts = localIp.split(".")
        val baseIp = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}"

        val hosts = mutableListOf<ScannedHost>()
        val scanned = java.util.concurrent.atomic.AtomicInteger(0)
        val mutex = Mutex()

        // 并发扫描整个 /24 网段
        val jobs = (1..254).map { i ->
            launch {
                val targetIp = "$baseIp.$i"
                if (targetIp == localIp) {
                    scanned.incrementAndGet()
                    return@launch
                }

                val hasSmb = try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(targetIp, 445), timeoutMs)
                        true
                    }
                } catch (_: Exception) {
                    false
                }

                if (hasSmb) {
                    val hostname = try {
                        InetAddress.getByName(targetIp).hostName ?: targetIp
                    } catch (_: Exception) {
                        targetIp
                    }
                    mutex.withLock {
                        hosts.add(ScannedHost(targetIp, hostname, true))
                        onProgress(scanned.get(), hosts.toList())
                    }
                }
                scanned.incrementAndGet()
                // 每扫描 20 个 IP 更新一次进度
                if (scanned.get() % 20 == 0) {
                    onProgress(scanned.get(), hosts.toList())
                }
            }
        }

        jobs.forEach { it.join() }
        onProgress(254, hosts.toList())
        hosts.sortedBy { it.ip.split(".").last().toIntOrNull() ?: 0 }
    }
}
