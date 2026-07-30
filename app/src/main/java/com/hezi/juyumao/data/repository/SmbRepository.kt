package com.hezi.juyumao.data.repository

import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.entity.ServerEntity
import com.hezi.juyumao.data.remote.discovery.DiscoveredServer
import com.hezi.juyumao.data.remote.discovery.SmbDiscovery
import com.hezi.juyumao.data.remote.smb.SmbClientWrapper
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import com.hezi.juyumao.data.remote.smb.SmbConnectionState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbRepository @Inject constructor(
    private val serverDao: ServerDao,
    private val connectionPool: SmbConnectionPool,
) {
    private val discovery = SmbDiscovery()

    val connectionState: Flow<SmbConnectionState> = connectionPool.connectionState

    fun getAllServers(): Flow<List<ServerEntity>> = serverDao.getAllServers()

    fun getAutoConnectServers(): Flow<List<ServerEntity>> = serverDao.getAutoConnectServers()

    suspend fun connect(server: ServerEntity): Result<Unit> {
        return try {
            connectionPool.getConnection(
                serverId = server.id,
                host = server.ip,
                port = server.port,
                username = server.username,
                password = server.password,
                shareName = server.shareName,
            )
            serverDao.update(server.copy(lastConnectedAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveServer(server: ServerEntity): Long {
        return serverDao.insert(server)
    }

    suspend fun deleteServer(server: ServerEntity) {
        connectionPool.disconnect(server.id)
        serverDao.delete(server)
    }

    fun disconnect(serverId: Long) {
        connectionPool.disconnect(serverId)
    }

    fun disconnectAll() {
        connectionPool.disconnectAll()
    }

    suspend fun discoverServers(): Result<List<DiscoveredServer>> {
        return discovery.discover()
    }
}
