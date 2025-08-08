package com.wy.diary.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * 日记应用数据库
 */
@Database(
    entities = [DiaryEntity::class, CacheMetadataEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun cacheMetadataDao(): CacheMetadataDao

    companion object {
        private const val DATABASE_NAME = "diary_database"

        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        fun getInstance(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // 简化版本升级
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}