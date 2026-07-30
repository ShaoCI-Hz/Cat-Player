package com.hezi.juyumao.data.repository

import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.local.scanner.LocalMusicScanner
import com.hezi.juyumao.data.remote.smb.SmbClientWrapper
import com.hezi.juyumao.data.remote.smb.SmbFileScanner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val songDao: SongDao,
    private val localMusicScanner: LocalMusicScanner,
) {
    private val smbScanner = SmbFileScanner()

    fun getAllSongsPaged() = songDao.getAllSongsPaged()

    fun getRecentlyPlayed() = songDao.getRecentlyPlayed()

    fun search(query: String) = songDao.search(query)

    fun getSongCount() = songDao.getSongCount()

    fun getTotalSize() = songDao.getTotalSize()

    suspend fun scanSmbDirectory(smbClient: SmbClientWrapper, path: String, serverId: Long): Result<Int> {
        val result = smbScanner.scanDirectory(smbClient, path, serverId)
        return result.map { songs ->
            songDao.insertAll(songs)
            songs.size
        }
    }

    suspend fun scanLocalMusic(): Result<Int> {
        return try {
            val result = localMusicScanner.scanAllMusic()
            result.map { songs ->
                // 先清除旧的本地歌曲，再插入新的（全量刷新）
                songDao.deleteBySource("LOCAL")
                // 分批插入，每批 200 条
                songs.chunked(200).forEach { batch ->
                    songDao.insertAll(batch)
                }
                songs.size
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertSong(song: SongEntity) = songDao.insert(song)

    suspend fun updateSong(song: SongEntity) = songDao.update(song)

    suspend fun deleteSong(song: SongEntity) = songDao.delete(song)
}
