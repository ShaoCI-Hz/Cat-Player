package com.hezi.juyumao.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN albumArtist TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE songs ADD COLUMN trackNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN discNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN year INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN genre TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE songs ADD COLUMN composer TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE songs ADD COLUMN bitrate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN sampleRate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN bitsPerSample INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN hasEmbeddedLyrics INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN hasExternalLyrics INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                // 先去重再建索引，避免重复数据导致崩溃
                db.execSQL("DELETE FROM songs WHERE rowid NOT IN (SELECT MIN(rowid) FROM songs GROUP BY filePath)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_songs_filePath ON songs(filePath)")
            }
        }

        fun create(context: Context): JuYuMaoDatabase {
            return Room.databaseBuilder(
                context,
                JuYuMaoDatabase::class.java,
                "juyumao.db",
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
