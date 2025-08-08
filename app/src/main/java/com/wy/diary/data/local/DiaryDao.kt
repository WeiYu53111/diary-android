package com.wy.diary.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 日记数据访问对象
 */
@Dao
interface DiaryDao {
    
    /**
     * 获取所有日记 - 按创建时间倒序
     */
    @Query("SELECT * FROM diary_entries WHERE isDeleted = 0 ORDER BY createTime DESC")
    fun getAllDiariesFlow(): Flow<List<DiaryEntity>>
    
    /**
     * 分页获取日记
     */
    @Query("SELECT * FROM diary_entries WHERE isDeleted = 0 ORDER BY createTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getDiariesPaged(limit: Int, offset: Int): List<DiaryEntity>
    
    /**
     * 根据ID获取日记
     */
    @Query("SELECT * FROM diary_entries WHERE diaryId = :diaryId AND isDeleted = 0")
    suspend fun getDiaryById(diaryId: String): DiaryEntity?
    
    /**
     * 插入或更新日记
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity)
    
    /**
     * 批量插入或更新日记
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaries(diaries: List<DiaryEntity>)
    
    /**
     * 软删除日记
     */
    @Query("UPDATE diary_entries SET isDeleted = 1 WHERE diaryId = :diaryId")
    suspend fun softDeleteDiary(diaryId: String)
    
    /**
     * 物理删除日记
     */
    @Query("DELETE FROM diary_entries WHERE diaryId = :diaryId")
    suspend fun deleteDiary(diaryId: String)
    
    /**
     * 清空所有日记
     */
    @Query("DELETE FROM diary_entries")
    suspend fun clearAllDiaries()
    
    /**
     * 获取日记总数
     */
    @Query("SELECT COUNT(*) FROM diary_entries WHERE isDeleted = 0")
    suspend fun getDiaryCount(): Int
    
    /**
     * 获取最后更新时间大于指定时间的日记
     */
    @Query("SELECT * FROM diary_entries WHERE lastUpdateTime > :timestamp ORDER BY createTime DESC")
    suspend fun getDiariesUpdatedAfter(timestamp: Long): List<DiaryEntity>
}

/**
 * 缓存元数据数据访问对象
 */
@Dao
interface CacheMetadataDao {
    
    @Query("SELECT * FROM cache_metadata WHERE key = :key")
    suspend fun getCacheMetadata(key: String): CacheMetadataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheMetadata(metadata: CacheMetadataEntity)
    
    @Query("DELETE FROM cache_metadata WHERE key = :key")
    suspend fun deleteCacheMetadata(key: String)
    
    @Query("DELETE FROM cache_metadata WHERE expiryTime < :currentTime")
    suspend fun deleteExpiredCache(currentTime: Long)
}