package com.hezi.juyumao.data.repository

import androidx.room.withTransaction
import com.hezi.juyumao.data.local.db.JuYuMaoDatabase
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
    private val database: JuYuMaoDatabase,
    private val songDao: SongDao,
    private val localMusicScanner: LocalMusicScanner,
    private val smbScanner: SmbFileScanner,
) {

    fun getAllSongsPaged() = songDao.getAllSongsPaged()

    fun getRecentlyPlayed() = songDao.getRecentlyPlayed()

    fun search(query: String) = songDao.search(query)

    fun getSongCount() = songDao.getSongCount()

    fun getTotalSize() = songDao.getTotalSize()

    fun getAlbumCount() = songDao.getAlbumCount()

    fun getArtistCount() = songDao.getArtistCount()

    fun getFavorites() = songDao.getFavorites()

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) =
        songDao.updateFavorite(songId, isFavorite)

    // ── 播放统计（T10.10） ──

    fun getTotalPlayCount() = songDao.getTotalPlayCount()

    fun getTopPlayedSongs(limit: Int) = songDao.getTopPlayedSongs(limit)

    fun getSongsPlayedSince(since: Long) = songDao.getSongsPlayedSince(since)

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
                // 事务保证删除旧数据和插入新数据的原子性
                database.withTransaction {
                    songDao.deleteBySource("LOCAL")
                    songs.chunked(200).forEach { batch ->
                        songDao.insertAll(batch)
                    }
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
