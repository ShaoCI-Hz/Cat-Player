package com.hezi.juyumao.data.remote.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NetworkState {
    WIFI, CELLULAR, DISCONNECTED, OTHER,
}

class NetworkMonitor(private val context: Context) {

    val networkState: Flow<NetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getNetworkState(connectivityManager, network))
            }

            override fun onLost(network: Network) {
                trySend(NetworkState.DISCONNECTED)
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(getNetworkState(connectivityManager, network))
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, callback)

        trySend(getCurrentState(connectivityManager))

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun getNetworkState(cm: ConnectivityManager, network: Network): NetworkState {
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkState.OTHER
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.CELLULAR
            else -> NetworkState.OTHER
        }
    }

    private fun getCurrentState(cm: ConnectivityManager): NetworkState {
        val network = cm.activeNetwork ?: return NetworkState.DISCONNECTED
        return getNetworkState(cm, network)
    }
}
