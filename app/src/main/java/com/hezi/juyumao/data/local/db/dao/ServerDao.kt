package com.hezi.juyumao.data.local.db.dao

import androidx.room.*
import com.hezi.juyumao.data.local.db.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY lastConnectedAt DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE autoConnect = 1")
    fun getAutoConnectServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: Long): ServerEntity?

    @Upsert
    suspend fun insert(server: ServerEntity): Long

    @Update
    suspend fun update(server: ServerEntity)

    @Delete
    suspend fun delete(server: ServerEntity)

    @Query("UPDATE servers SET connectionError = :error WHERE id = :serverId")
    suspend fun updateConnectionError(serverId: Long, error: String?)

    @Query("UPDATE servers SET isOnline = :online WHERE id = :serverId")
    suspend fun updateOnlineStatus(serverId: Long, online: Boolean)

    @Query("UPDATE servers SET lastScanAt = :time, scannedSongCount = :count WHERE id = :serverId")
    suspend fun updateScanInfo(serverId: Long, time: Long, count: Int)
}
