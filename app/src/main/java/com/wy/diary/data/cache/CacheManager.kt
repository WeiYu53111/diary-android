package com.wy.diary.data.cache

import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存管理器 - 负责缓存策略和后台同步
 */
@Singleton
class CacheManager @Inject constructor(
    private val workManager: WorkManager
) {

    companion object {
        private const val TAG = "CacheManager"
        private const val CACHE_SYNC_WORK_NAME = "cache_sync_work"
        private const val CACHE_CLEANUP_WORK_NAME = "cache_cleanup_work"
    }

    /**
     * 启动周期性缓存同步
     */
    fun startPeriodicCacheSync() {
        Log.d(TAG, "启动周期性缓存同步")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CacheSyncWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            CACHE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * 启动周期性缓存清理
     */
    fun startPeriodicCacheCleanup() {
        Log.d(TAG, "启动周期性缓存清理")
        
        val cleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(2, TimeUnit.HOURS) // 启动后2小时开始第一次清理
            .build()

        workManager.enqueueUniquePeriodicWork(
            CACHE_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    /**
     * 手动触发缓存同步
     */
    fun triggerImmediateCacheSync() {
        Log.d(TAG, "手动触发缓存同步")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<CacheSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
    }

    /**
     * 停止所有缓存相关的后台任务
     */
    fun stopCacheWork() {
        Log.d(TAG, "停止缓存相关后台任务")
        workManager.cancelUniqueWork(CACHE_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(CACHE_CLEANUP_WORK_NAME)
    }

    /**
     * 获取缓存同步状态
     */
    fun getCacheSyncStatus() = workManager.getWorkInfosForUniqueWorkLiveData(CACHE_SYNC_WORK_NAME)
}

/**
 * 缓存同步Worker
 */
class CacheSyncWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val diaryRepositoryWithCache: com.wy.diary.data.repository.DiaryRepositoryWithCache
) : CoroutineWorker(context, workerParams) {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(context: android.content.Context, params: WorkerParameters): CacheSyncWorker
    }

    override suspend fun doWork(): Result {
        Log.d("CacheSyncWorker", "开始缓存同步")
        
        return try {
            val result = diaryRepositoryWithCache.refreshCache(50) // 同步前50条数据
            
            if (result.isSuccess) {
                Log.d("CacheSyncWorker", "缓存同步成功")
                Result.success()
            } else {
                Log.e("CacheSyncWorker", "缓存同步失败")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("CacheSyncWorker", "缓存同步异常", e)
            Result.failure()
        }
    }
}

/**
 * 缓存清理Worker
 */
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val diaryRepositoryWithCache: com.wy.diary.data.repository.DiaryRepositoryWithCache
) : CoroutineWorker(context, workerParams) {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(context: android.content.Context, params: WorkerParameters): CacheCleanupWorker
    }

    override suspend fun doWork(): Result {
        Log.d("CacheCleanupWorker", "开始缓存清理")
        
        return try {
            diaryRepositoryWithCache.cleanExpiredCache()
            Log.d("CacheCleanupWorker", "缓存清理完成")
            Result.success()
        } catch (e: Exception) {
            Log.e("CacheCleanupWorker", "缓存清理异常", e)
            Result.failure()
        }
    }
}