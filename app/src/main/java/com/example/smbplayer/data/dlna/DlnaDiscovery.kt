package com.example.smbplayer.data.dlna

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DLNA/UPnP device discovery via SSDP.
 */
@Singleton
class DlnaDiscovery @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _devices = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val devices: StateFlow<List<DlnaDevice>> = _devices.asStateFlow()

    private var discoveryJob: Job? = null

    companion object {
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SEARCH_TARGET = "urn:schemas-upnp-org:device:MediaRenderer:1"
    }

    /**
     * Start discovering DLNA devices on the local network.
     */
    fun startDiscovery() {
        stopDiscovery()
        discoveryJob = scope.launch {
            while (isActive) {
                discoverDevices()
                delay(30000) // Rediscover every 30 seconds
            }
        }
    }

    /**
     * Stop discovery.
     */
    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    private suspend fun discoverDevices() {
        withContext(Dispatchers.IO) {
            try {
                val socket = MulticastSocket(SSDP_PORT)
                val group = InetAddress.getByName(SSDP_ADDRESS)

                // Join multicast group
                val networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
                if (networkInterface != null) {
                    socket.joinGroup(group)
                }

                // Send M-SEARCH request
                val searchRequest = buildString {
                    append("M-SEARCH * HTTP/1.1\r\n")
                    append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
                    append("MAN: \"ssdp:discover\"\r\n")
                    append("MX: 3\r\n")
                    append("ST: $SEARCH_TARGET\r\n")
                    append("\r\n")
                }

                val data = searchRequest.toByteArray()
                val packet = DatagramPacket(data, data.size, group, SSDP_PORT)
                socket.send(packet)

                // Listen for responses
                val buffer = ByteArray(4096)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                socket.soTimeout = 5000 // 5 second timeout

                val discoveredDevices = mutableListOf<DlnaDevice>()

                try {
                    while (true) {
                        socket.receive(responsePacket)
                        val response = String(responsePacket.data, 0, responsePacket.length)
                        val device = parseSsdpResponse(response, responsePacket.address.hostAddress ?: "")
                        if (device != null) {
                            discoveredDevices.add(device)
                        }
                    }
                } catch (_: Exception) {
                    // Timeout - expected
                }

                socket.leaveGroup(group)
                socket.close()

                if (discoveredDevices.isNotEmpty()) {
                    _devices.value = discoveredDevices
                }

            } catch (_: Exception) {
                // Discovery failed - network issue
            }
        }
    }

    private fun parseSsdpResponse(response: String, ip: String): DlnaDevice? {
        val headers = response.lines().associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim()
            else "" to ""
        }

        val location = headers["LOCATION"] ?: return null
        val usn = headers["USN"] ?: ""
        val server = headers["SERVER"] ?: ""

        return DlnaDevice(
            ip = ip,
            location = location,
            usn = usn,
            server = server,
            friendlyName = server // Will be updated after fetching device description
        )
    }

    fun release() {
        stopDiscovery()
        scope.cancel()
    }
}

data class DlnaDevice(
    val ip: String,
    val location: String,
    val usn: String,
    val server: String,
    val friendlyName: String
)
