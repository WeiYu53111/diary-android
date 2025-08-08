package com.wy.diary.data.repository

import android.util.Log
import com.wy.diary.data.local.CacheMetadataEntity
import com.wy.diary.data.local.DiaryDao
import com.wy.diary.data.local.DiaryEntity
import com.wy.diary.data.local.CacheMetadataDao
import com.wy.diary.data.model.DiaryListPagedData
import com.wy.diary.data.model.DiaryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地缓存Repository
 */
@Singleton
class DiaryLocalRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val cacheMetadataDao: CacheMetadataDao
) {

    companion object {
        private const val TAG = "DiaryLocalRepository"
        private const val CACHE_KEY_DIARY_LIST = "diary_list"
        private const val CACHE_EXPIRY_DURATION = 24 * 60 * 60 * 1000L // 24小时
    }

    /**
     * 获取所有缓存的日记Flow
     */
    fun getAllDiariesFlow(): Flow<List<DiaryRecord>> {
        return diaryDao.getAllDiariesFlow().map { entities ->
            entities.map { it.toDiaryRecord() }
        }
    }

    /**
     * 分页获取缓存的日记
     */
    suspend fun getDiariesPaged(pageIndex: Int, pageSize: Int): DiaryListPagedData {
        Log.d(TAG, "从本地缓存获取日记列表: 页码=$pageIndex, 每页数量=$pageSize")
        
        val offset = (pageIndex - 1) * pageSize
        val diaries = diaryDao.getDiariesPaged(pageSize, offset)
        val totalCount = diaryDao.getDiaryCount()
        val totalPages = (totalCount + pageSize - 1) / pageSize
        val hasNext = pageIndex < totalPages

        val records = diaries.map { it.toDiaryRecord() }
        
        Log.d(TAG, "本地缓存返回: ${records.size} 条记录，总计 $totalCount 条")
        
        return DiaryListPagedData(
            records = records,
            totalCount = totalCount,
            totalPages = totalPages,
            hasNext = hasNext
        )
    }

    /**
     * 根据ID获取日记
     */
    suspend fun getDiaryById(diaryId: String): DiaryRecord? {
        return diaryDao.getDiaryById(diaryId)?.toDiaryRecord()
    }

    /**
     * 缓存单个日记
     */
    suspend fun cacheDiary(record: DiaryRecord) {
        Log.d(TAG, "缓存日记: ${record.diaryId}")
        val entity = record.toDiaryEntity()
        diaryDao.insertDiary(entity)
    }

    /**
     * 批量缓存日记
     */
    suspend fun cacheDiaries(records: List<DiaryRecord>) {
        Log.d(TAG, "批量缓存日记: ${records.size} 条")
        val entities = records.map { it.toDiaryEntity() }
        diaryDao.insertDiaries(entities)
        
        // 更新缓存元数据
        updateCacheMetadata(records.size)
    }

    /**
     * 删除缓存的日记
     */
    suspend fun deleteCachedDiary(diaryId: String) {
        Log.d(TAG, "删除缓存日记: $diaryId")
        diaryDao.softDeleteDiary(diaryId)
    }

    /**
     * 清空所有缓存
     */
    suspend fun clearAllCache() {
        Log.d(TAG, "清空所有缓存")
        diaryDao.clearAllDiaries()
        cacheMetadataDao.deleteCacheMetadata(CACHE_KEY_DIARY_LIST)
    }

    /**
     * 检查缓存是否有效
     */
    suspend fun isCacheValid(): Boolean {
        val metadata = cacheMetadataDao.getCacheMetadata(CACHE_KEY_DIARY_LIST)
        val currentTime = System.currentTimeMillis()
        
        return metadata != null && metadata.expiryTime > currentTime
    }

    /**
     * 检查是否有缓存数据
     */
    suspend fun hasCachedData(): Boolean {
        val count = diaryDao.getDiaryCount()
        Log.d(TAG, "缓存中有 $count 条日记")
        return count > 0
    }

    /**
     * 获取缓存的最后更新时间
     */
    suspend fun getLastUpdateTime(): Long {
        return cacheMetadataDao.getCacheMetadata(CACHE_KEY_DIARY_LIST)?.lastUpdateTime ?: 0L
    }

    /**
     * 更新缓存元数据
     */
    private suspend fun updateCacheMetadata(recordCount: Int) {
        val currentTime = System.currentTimeMillis()
        val metadata = CacheMetadataEntity(
            key = CACHE_KEY_DIARY_LIST,
            lastUpdateTime = currentTime,
            totalCount = recordCount,
            expiryTime = currentTime + CACHE_EXPIRY_DURATION
        )
        cacheMetadataDao.insertCacheMetadata(metadata)
        Log.d(TAG, "更新缓存元数据: $recordCount 条记录")
    }

    /**
     * 清理过期缓存
     */
    suspend fun cleanExpiredCache() {
        val currentTime = System.currentTimeMillis()
        cacheMetadataDao.deleteExpiredCache(currentTime)
        Log.d(TAG, "清理过期缓存完成")
    }
}

/**
 * DiaryRecord 扩展函数 - 转换为 DiaryEntity
 */
private fun DiaryRecord.toDiaryEntity(): DiaryEntity {
    return DiaryEntity(
        diaryId = this.diaryId,
        logTime = this.logTime,
        logWeek = this.logWeek,
        logLunar = this.logLunar,
        editorContent = this.editorContent,
        imageUrls = this.imageUrls,
        createTime = this.createTime,
        address = this.address,
        lastUpdateTime = System.currentTimeMillis(),
        isDeleted = false
    )
}

/**
 * DiaryEntity 扩展函数 - 转换为 DiaryRecord
 */
private fun DiaryEntity.toDiaryRecord(): DiaryRecord {
    return DiaryRecord(
        diaryId = this.diaryId,
        logTime = this.logTime,
        logWeek = this.logWeek,
        logLunar = this.logLunar,
        editorContent = this.editorContent,
        imageUrls = this.imageUrls,
        createTime = this.createTime,
        address = this.address
    )
}