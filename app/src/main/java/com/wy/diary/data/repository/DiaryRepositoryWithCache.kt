package com.wy.diary.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.wy.diary.data.model.DiaryListPagedData
import com.wy.diary.data.model.DiaryRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 带缓存功能的日记Repository实现
 * 采用 Cache-First 策略，优先从缓存读取，缓存为空时从网络获取
 */
@Singleton
class DiaryRepositoryWithCache @Inject constructor(
    private val networkRepository: DiaryRepositoryHttpImpl,
    private val localRepository: DiaryLocalRepository
) : DiaryRepository {

    companion object {
        private const val TAG = "DiaryRepositoryWithCache"
    }

    /**
     * 获取日记ID - 直接调用网络接口
     */
    override suspend fun getDiaryId(): Result<String> {
        return networkRepository.getDiaryId()
    }

    /**
     * 上传图片 - 直接调用网络接口
     */
    override suspend fun uploadImage(context: Context, photoUri: Uri, diaryId: String): Result<String> {
        return networkRepository.uploadImage(context, photoUri, diaryId)
    }

    /**
     * 保存日记 - 先保存到网络，成功后更新缓存
     */
    override suspend fun saveDiary(request: DiaryRequest): Result<String> {
        Log.d(TAG, "保存日记到网络")
        
        return try {
            val result = networkRepository.saveDiary(request)
            
            if (result.isSuccess) {
                Log.d(TAG, "日记保存成功，刷新缓存")
                // 保存成功后，异步刷新缓存
                refreshCacheInBackground()
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "保存日记失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取日记列表 - Cache-First策略
     */
    override suspend fun getDiaryList(pageIndex: Int, pageSize: Int): Result<DiaryListPagedData> {
        Log.d(TAG, "获取日记列表: 页码=$pageIndex, 每页数量=$pageSize")

        return try {
            // 1. 检查是否有有效缓存
            if (localRepository.hasCachedData() && localRepository.isCacheValid()) {
                Log.d(TAG, "使用本地缓存数据")
                val cachedData = localRepository.getDiariesPaged(pageIndex, pageSize)
                return Result.success(cachedData)
            }

            // 2. 缓存无效或为空，从网络获取
            Log.d(TAG, "缓存无效，从网络获取数据")
            val networkResult = networkRepository.getDiaryList(pageIndex, pageSize)
            
            if (networkResult.isSuccess) {
                val networkData = networkResult.getOrNull()
                if (networkData != null && networkData.records.isNotEmpty()) {
                    // 3. 缓存网络数据
                    Log.d(TAG, "缓存网络数据")
                    if (pageIndex == 1) {
                        // 第一页时清空旧缓存
                        localRepository.clearAllCache()
                    }
                    localRepository.cacheDiaries(networkData.records)
                }
                return networkResult
            } else {
                // 4. 网络请求失败，尝试返回缓存数据
                if (localRepository.hasCachedData()) {
                    Log.d(TAG, "网络失败，返回缓存数据")
                    val cachedData = localRepository.getDiariesPaged(pageIndex, pageSize)
                    return Result.success(cachedData)
                } else {
                    Log.e(TAG, "网络失败且无缓存数据")
                    return networkResult
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取日记列表异常", e)
            // 异常情况下尝试返回缓存数据
            if (localRepository.hasCachedData()) {
                Log.d(TAG, "异常情况下返回缓存数据")
                val cachedData = localRepository.getDiariesPaged(pageIndex, pageSize)
                return Result.success(cachedData)
            } else {
                return Result.failure(e)
            }
        }
    }

    /**
     * 删除日记 - 先删除网络数据，成功后删除缓存
     */
    override suspend fun deleteDiary(diaryId: String, createTime: String): Result<Boolean> {
        Log.d(TAG, "删除日记: $diaryId")
        
        return try {
            val result = networkRepository.deleteDiary(diaryId, createTime)
            
            if (result.isSuccess) {
                Log.d(TAG, "网络删除成功，删除本地缓存")
                localRepository.deleteCachedDiary(diaryId)
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "删除日记失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取缓存的日记列表Flow - 用于实时更新UI
     */
    fun getCachedDiariesFlow() = localRepository.getAllDiariesFlow()

    /**
     * 强制刷新缓存
     */
    suspend fun refreshCache(pageSize: Int = 20): Result<Unit> {
        Log.d(TAG, "强制刷新缓存")
        
        return try {
            // 清空现有缓存
            localRepository.clearAllCache()
            
            // 获取第一页数据
            val result = networkRepository.getDiaryList(1, pageSize)
            
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null && data.records.isNotEmpty()) {
                    localRepository.cacheDiaries(data.records)
                    Log.d(TAG, "缓存刷新成功")
                }
                Result.success(Unit)
            } else {
                Log.e(TAG, "缓存刷新失败")
                Result.failure(Exception("刷新缓存失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "缓存刷新异常", e)
            Result.failure(e)
        }
    }

    /**
     * 后台异步刷新缓存
     */
    private suspend fun refreshCacheInBackground() {
        try {
            refreshCache()
        } catch (e: Exception) {
            Log.e(TAG, "后台刷新缓存失败", e)
        }
    }

    /**
     * 清理过期缓存
     */
    suspend fun cleanExpiredCache() {
        localRepository.cleanExpiredCache()
    }

    /**
     * 检查缓存状态
     */
    suspend fun getCacheInfo(): CacheInfo {
        return CacheInfo(
            hasData = localRepository.hasCachedData(),
            isValid = localRepository.isCacheValid(),
            lastUpdateTime = localRepository.getLastUpdateTime()
        )
    }
}

/**
 * 缓存信息数据类
 */
data class CacheInfo(
    val hasData: Boolean,
    val isValid: Boolean,
    val lastUpdateTime: Long
)