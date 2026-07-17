package com.example.smbplayer.domain

import com.example.smbplayer.data.smb.SmbConfig
import com.example.smbplayer.data.smb.SmbConnectionManager
import javax.inject.Inject

class ConnectUseCase @Inject constructor(
    private val connectionManager: SmbConnectionManager
) {
    suspend operator fun invoke(config: SmbConfig): Result<Unit> =
        connectionManager.connect(config)

    suspend fun disconnect() = connectionManager.disconnect()
}
