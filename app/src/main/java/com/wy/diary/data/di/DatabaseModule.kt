package com.wy.diary.data.di

import android.content.Context
import androidx.room.Room
import com.wy.diary.data.local.CacheMetadataDao
import com.wy.diary.data.local.DiaryDao
import com.wy.diary.data.local.DiaryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库相关依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiaryDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            DiaryDatabase::class.java,
            "diary_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDiaryDao(database: DiaryDatabase): DiaryDao {
        return database.diaryDao()
    }

    @Provides
    fun provideCacheMetadataDao(database: DiaryDatabase): CacheMetadataDao {
        return database.cacheMetadataDao()
    }
}