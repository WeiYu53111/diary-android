package com.wy.diary.data.di

import android.content.Context
import androidx.work.WorkManager
import com.wy.diary.data.cache.CacheCleanupWorker
import com.wy.diary.data.cache.CacheSyncWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WorkManager和Worker相关依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}

/**
 * Worker工厂模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerBindingModule {

    @dagger.Binds
    @dagger.multibindings.IntoMap
    @dagger.multibindings.ClassKey(CacheSyncWorker::class)
    abstract fun bindCacheSyncWorkerFactory(factory: CacheSyncWorker.Factory): dagger.assisted.AssistedFactory<*, *>

    @dagger.Binds
    @dagger.multibindings.IntoMap
    @dagger.multibindings.ClassKey(CacheCleanupWorker::class)
    abstract fun bindCacheCleanupWorkerFactory(factory: CacheCleanupWorker.Factory): dagger.assisted.AssistedFactory<*, *>
}