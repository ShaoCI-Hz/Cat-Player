package com.hezi.juyumao.data.remote.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val domain: String = "",
)

class SmbDiscovery {

    @Volatile private var jmdns: JmDNS? = null
    @Volatile private var listener: ServiceListener? = null

    suspend fun discover(timeoutMs: Long = 5000): Result<List<DiscoveredServer>> =
        withContext(Dispatchers.IO) {
            try {
                // 线程安全列表，回调中可能并发写入
                val servers = CopyOnWriteArrayList<DiscoveredServer>()

                synchronized(this@SmbDiscovery) {
                    jmdns = JmDNS.create(InetAddress.getLocalHost())
                    listener = object : ServiceListener {
                        override fun serviceAdded(event: ServiceEvent) {
                            synchronized(this@SmbDiscovery) {
                                jmdns?.requestServiceInfo(event.type, event.name, true)
                            }
                        }

                        override fun serviceRemoved(event: ServiceEvent) {}

                        override fun serviceResolved(event: ServiceEvent) {
                            servers.add(
                                DiscoveredServer(
                                    name = event.info.name,
                                    host = event.info.hostAddresses.firstOrNull() ?: "",
                                    port = event.info.port,
                                    domain = event.info.domain,
                                )
                            )
                        }
                    }
                    jmdns?.addServiceListener("_smb._tcp.local.", listener)
                }

                delay(timeoutMs)
                cleanup()

                Result.success(servers.toList())
            } catch (e: Exception) {
                cleanup()
                Result.failure(e)
            }
        }

    fun stop() {
        cleanup()
    }

    private fun cleanup() {
        synchronized(this) {
            try {
                val l = listener
                val j = jmdns
                if (l != null && j != null) {
                    j.removeServiceListener("_smb._tcp.local.", l)
                }
                j?.close()
            } catch (_: Exception) {}
            listener = null
            jmdns = null
        }
    }
}
