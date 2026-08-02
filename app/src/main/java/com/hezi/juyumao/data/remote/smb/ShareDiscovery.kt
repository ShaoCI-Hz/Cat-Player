package com.hezi.juyumao.data.remote.smb

import android.util.Log
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShareDiscovery @Inject constructor(
    private val jcifsEnumerator: JcifsShareEnumerator,
) {

    private val commonShareNames = listOf(
        "public", "share", "shared", "media", "music", "video", "videos",
        "photos", "pictures", "documents", "downloads", "data", "storage",
        "nas", "home", "homes", "multimedia", "usb", "sata", "hdd",
        "backup", "backups", "library", "files", "common", "netshare",
        "USB_Storage", "usb_storage", "sda1", "sdb1", "sda2", "sdb2",
        "XiaoMi", "xiaomi", "MiShare", "mishare", "mi_share",
        "photo", "web", "NetBackup", "Multimedia", "Public", "Download", "Web",
        "ai_disk", "AiDisk", "asus", "router",
        "vol1", "vol2", "volume1", "volume2",
        "Disk1", "Disk2", "disk1", "disk2",
        "RAID1", "raid1", "Storage", "STORAGE",
        "Media", "MEDIA", "Music", "MUSIC", "Video", "VIDEO",
        "Photo", "PHOTO", "Photos", "PHOTOS",
        "File", "FILE", "Files", "FILES",
        "Shared", "SHARED", "PUBLIC", "Public",
        "temp", "tmp", "ftp",
        "影音", "音乐", "视频", "照片", "文档", "下载", "共享",
        "存储", "备份", "媒体", "文件",
        "我的文档", "我的音乐", "我的视频", "我的照片",
        "小米", "MINAS", "minas",
    )

    data class DiscoveredShare(
        val name: String,
        val accessible: Boolean = true,
    )

    suspend fun discoverShares(
        host: String,
        port: Int = 445,
        username: String = "",
        password: String = "",
        domain: String = "",
    ): List<DiscoveredShare> = withContext(Dispatchers.IO) {
        Log.d("ShareDiscovery", "开始搜索共享: $host:$port")

        // 方法1: jcifs-ng 枚举（smb://host/ 列目录，和 Windows 资源管理器一样）
        try {
            val jcifsShares = jcifsEnumerator.enumerateShares(host, port, username, password, domain)
            if (jcifsShares.isNotEmpty()) {
                Log.d("ShareDiscovery", "jcifs-ng 枚举到 ${jcifsShares.size} 个共享")
                return@withContext jcifsShares.map { DiscoveredShare(it.name, true) }
            }
        } catch (e: Exception) {
            Log.d("ShareDiscovery", "jcifs-ng 枚举失败: ${e.message}")
        }

        // 方法3: 暴力搜索常见共享名
        val shares = mutableListOf<DiscoveredShare>()
        try {
            val config = SmbConfig.builder()
                .withTimeout(5, TimeUnit.SECONDS)
                .withSoTimeout(5, TimeUnit.SECONDS)
                .build()
            val client = SMBClient(config)
            val connection = client.connect(host, port)
            val ac = if (username.isEmpty()) AuthenticationContext.anonymous()
            else AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(ac)
            Log.d("ShareDiscovery", "暴力测试 ${commonShareNames.size} 个共享名...")

            for (shareName in commonShareNames) {
                try {
                    val share = session.connectShare(shareName)
                    share.close()
                    shares.add(DiscoveredShare(shareName, true))
                    Log.d("ShareDiscovery", "✓ $shareName")
                } catch (_: Exception) {}
            }

            session.close()
            connection.close()
            client.close()
        } catch (e: Exception) {
            Log.e("ShareDiscovery", "连接失败", e)
        }

        Log.d("ShareDiscovery", "搜索完成，发现 ${shares.size} 个共享")
        shares
    }

    suspend fun testShare(
        host: String, port: Int = 445,
        username: String = "", password: String = "",
        shareName: String, domain: String = "",
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val config = SmbConfig.builder()
                .withTimeout(5, TimeUnit.SECONDS)
                .withSoTimeout(5, TimeUnit.SECONDS)
                .build()
            val client = SMBClient(config)
            val connection = client.connect(host, port)
            val ac = if (username.isEmpty()) AuthenticationContext.anonymous()
            else AuthenticationContext(username, password.toCharArray(), domain)
            val session = connection.authenticate(ac)
            val share = session.connectShare(shareName)
            share.close()
            session.close()
            connection.close()
            client.close()
            true
        } catch (_: Exception) {
            false
        }
    }
}
