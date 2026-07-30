package com.hezi.juyumao.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hezi.juyumao.data.local.db.dao.ServerDao
import com.hezi.juyumao.data.local.db.dao.SongDao
import com.hezi.juyumao.data.local.db.entity.PlaylistEntity
import com.hezi.juyumao.data.local.db.entity.ServerEntity
import com.hezi.juyumao.data.local.db.entity.SongEntity

@Database(
    entities = [SongEntity::class, ServerEntity::class, PlaylistEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class JuYuMaoDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun serverDao(): ServerDao

    companion object {
        fun create(context: Context): JuYuMaoDatabase {
            return Room.databaseBuilder(
                context,
                JuYuMaoDatabase::class.java,
                "juyumao.db",
            ).fallbackToDestructiveMigration().build()
        }
    }
}
