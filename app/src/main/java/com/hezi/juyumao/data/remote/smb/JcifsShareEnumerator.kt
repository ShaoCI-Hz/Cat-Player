package com.hezi.juyumao.data.remote.smb

import android.util.Log
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject

/**
 * 使用 jcifs-ng 枚举 SMB 共享名（SMBJ 不支持，jcifs-ng 通过 smb://host/ 列目录实现）
 */
class JcifsShareEnumerator @Inject constructor() {

    data class JcifsShare(
        val name: String,
        val comment: String = "",
    )

    suspend fun enumerateShares(
        host: String,
        port: Int = 445,
        username: String = "",
        password: String = "",
        domain: String = "",
    ): List<JcifsShare> = withContext(Dispatchers.IO) {
        try {
            // jcifs-ng 配置：启用 SMB2/3，设置超时
            val props = Properties().apply {
                setProperty("jcifs.smb.client.enableSMB2", "true")
                setProperty("jcifs.smb.client.enableSMB1", "true")
                setProperty("jcifs.smb.client.responseTimeout", "10000")
                setProperty("jcifs.smb.client.soTimeout", "10000")
                setProperty("jcifs.smb.client.connTimeout", "10000")
            }
            val context: CIFSContext = BaseContext(PropertyConfiguration(props))

            val authContext = if (username.isNotEmpty()) {
                context.withCredentials(NtlmPasswordAuthenticator(domain, username, password))
            } else {
                // 匿名：jcifs-ng 默认 guest，传空凭据
                context
            }

            // 列出 smb://host/ 下的所有共享
            val rootUrl = "smb://$host/"
            Log.d("JcifsEnum", "列出共享: $rootUrl")
            val dir = SmbFile(rootUrl, authContext)
            val files = dir.listFiles()

            val shares = files.mapNotNull { f ->
                val name = f.name.trimEnd('/')
                if (name.isEmpty()) return@mapNotNull null
                JcifsShare(name = name)
            }
            Log.d("JcifsEnum", "发现 ${shares.size} 个共享: $shares")
            shares
        } catch (e: Exception) {
            Log.e("JcifsEnum", "jcifs-ng 枚举失败", e)
            emptyList()
        }
    }
}
