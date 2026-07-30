package com.hezi.juyumao.data.remote.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import java.net.InetAddress

data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val domain: String = "",
)

class SmbDiscovery {

    private var jmdns: JmDNS? = null

    suspend fun discover(timeoutMs: Long = 5000): Result<List<DiscoveredServer>> =
        withContext(Dispatchers.IO) {
            try {
                val servers = mutableListOf<DiscoveredServer>()
                val latch = java.util.concurrent.CountDownLatch(1)

                jmdns = JmDNS.create(InetAddress.getLocalHost())
                jmdns?.addServiceListener("_smb._tcp.local.", object : ServiceListener {
                    override fun serviceAdded(event: ServiceEvent) {
                        jmdns?.requestServiceInfo(event.type, event.name, true)
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
                })

                Thread.sleep(timeoutMs)
                jmdns?.removeAllServiceListeners()
                jmdns?.close()
                jmdns = null

                Result.success(servers)
            } catch (e: Exception) {
                jmdns?.close()
                jmdns = null
                Result.failure(e)
            }
        }

    fun stop() {
        try {
            jmdns?.removeAllServiceListeners()
            jmdns?.close()
        } catch (_: Exception) {}
        jmdns = null
    }
}
