package com.hezi.juyumao.data.repository

import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.remote.smb.SmbClient
import com.hezi.juyumao.data.remote.smb.SmbFileScanner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val songDao: SongDao,
) {
    private val scanner = SmbFileScanner()

    fun getAllSongsPaged() = songDao.getAllSongsPaged()

    fun getRecentlyPlayed() = songDao.getRecentlyPlayed()

    fun search(query: String) = songDao.search(query)

    fun getSongCount() = songDao.getSongCount()

    fun getTotalSize() = songDao.getTotalSize()

    suspend fun scanSmbDirectory(smbClient: SmbClient, path: String, serverId: Long): Result<Int> {
        val result = scanner.scanDirectory(smbClient, path, serverId)
        return result.map { songs ->
            songDao.insertAll(songs)
            songs.size
        }
    }

    suspend fun insertSong(song: SongEntity) = songDao.insert(song)

    suspend fun updateSong(song: SongEntity) = songDao.update(song)

    suspend fun deleteSong(song: SongEntity) = songDao.delete(song)
}
