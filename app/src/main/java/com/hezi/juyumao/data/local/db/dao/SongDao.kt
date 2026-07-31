package com.hezi.juyumao.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.hezi.juyumao.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

// 转义 LIKE 查询中的通配符
private fun String.escapeLike(): String = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY addedAt DESC")
    fun getAllSongsPaged(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY addedAt DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs ORDER BY lastPlayedAt DESC LIMIT 20")
    fun getRecentlyPlayed(): Flow<List<SongEntity>>

    // 搜索时先转义再用 ESCAPE '\' 匹配
    fun search(query: String): Flow<List<SongEntity>> {
        val escaped = query.escapeLike()
        return searchInternal("%$escaped%")
    }

    @Query("SELECT * FROM songs WHERE title LIKE :pattern ESCAPE '\\' OR artist LIKE :pattern ESCAPE '\\' OR album LIKE :pattern ESCAPE '\\'")
    fun searchInternal(pattern: String): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>

    @Query("SELECT SUM(fileSize) FROM songs")
    fun getTotalSize(): Flow<Long?>

    @Query("SELECT COUNT(DISTINCT album) FROM songs")
    fun getAlbumCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT artist) FROM songs WHERE artist != :unknownArtist")
    fun getArtistCount(unknownArtist: String = SongEntity.UNKNOWN_ARTIST): Flow<Int>

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<SongEntity>>

    // CRITICAL-6: 使用 Upsert 避免 REPLACE 删除再插入导致丢失 ID
    @Upsert
    suspend fun upsert(song: SongEntity)

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    // 保留旧方法名兼容，内部委托给 upsert
    suspend fun insert(song: SongEntity) = upsert(song)
    suspend fun insertAll(songs: List<SongEntity>) = upsertAll(songs)

    @Update
    suspend fun update(song: SongEntity)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("DELETE FROM songs WHERE source = :source")
    suspend fun deleteBySource(source: String)
}
