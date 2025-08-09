package com.wy.diary.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 图片缓存数据访问对象
 */
@Dao
interface ImageCacheDao {
    
    /**
     * 根据原始URL获取处理后的URL
     */
    @Query("SELECT * FROM image_cache_urls WHERE originalUrl = :originalUrl")
    suspend fun getCachedImageUrl(originalUrl: String): ImageCacheEntity?
    
    /**
     * 批量获取缓存的图片URL
     */
    @Query("SELECT * FROM image_cache_urls WHERE originalUrl IN (:originalUrls)")
    suspend fun getCachedImageUrls(originalUrls: List<String>): List<ImageCacheEntity>
    
    /**
     * 插入或更新图片URL缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageUrl(imageCache: ImageCacheEntity)
    
    /**
     * 批量插入图片URL缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageUrls(imageCaches: List<ImageCacheEntity>)
    
    /**
     * 更新最后访问时间
     */
    @Query("UPDATE image_cache_urls SET lastAccessTime = :accessTime WHERE originalUrl = :originalUrl")
    suspend fun updateLastAccessTime(originalUrl: String, accessTime: Long)
    
    /**
     * 删除过期的URL缓存
     */
    @Query("DELETE FROM image_cache_urls WHERE lastAccessTime < :expireTime")
    suspend fun deleteExpiredUrls(expireTime: Long): Int
    
    /**
     * 获取所有已预加载的图片
     */
    @Query("SELECT * FROM image_cache_urls WHERE isPreloaded = 1 ORDER BY lastAccessTime DESC")
    suspend fun getPreloadedImages(): List<ImageCacheEntity>
    
    /**
     * 标记图片为已预加载
     */
    @Query("UPDATE image_cache_urls SET isPreloaded = 1 WHERE originalUrl = :originalUrl")
    suspend fun markAsPreloaded(originalUrl: String)
    
    /**
     * 获取缓存统计信息
     */
    @Query("SELECT COUNT(*) FROM image_cache_urls")
    suspend fun getCacheCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM image_cache_urls")
    suspend fun getTotalCacheSize(): Long?
    
    /**
     * 清空所有URL缓存
     */
    @Query("DELETE FROM image_cache_urls")
    suspend fun clearAllCache()
}

/**
 * 图片预加载任务数据访问对象
 */
@Dao
interface ImagePreloadTaskDao {
    
    /**
     * 获取所有预加载任务
     */
    @Query("SELECT * FROM image_preload_tasks ORDER BY createTime DESC")
    fun getAllTasksFlow(): Flow<List<ImagePreloadTaskEntity>>
    
    /**
     * 根据状态获取任务
     */
    @Query("SELECT * FROM image_preload_tasks WHERE status = :status ORDER BY createTime ASC")
    suspend fun getTasksByStatus(status: PreloadStatus): List<ImagePreloadTaskEntity>
    
    /**
     * 根据任务ID获取任务
     */
    @Query("SELECT * FROM image_preload_tasks WHERE taskId = :taskId")
    suspend fun getTaskById(taskId: String): ImagePreloadTaskEntity?
    
    /**
     * 插入预加载任务
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ImagePreloadTaskEntity)
    
    /**
     * 更新任务状态
     */
    @Query("UPDATE image_preload_tasks SET status = :status, startTime = :startTime WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: PreloadStatus, startTime: Long? = null)
    
    /**
     * 更新任务进度
     */
    @Query("UPDATE image_preload_tasks SET progress = :progress, completedImages = :completedImages WHERE taskId = :taskId")
    suspend fun updateTaskProgress(taskId: String, progress: Int, completedImages: Int)
    
    /**
     * 完成任务
     */
    @Query("UPDATE image_preload_tasks SET status = :status, endTime = :endTime, progress = 100 WHERE taskId = :taskId")
    suspend fun completeTask(taskId: String, status: PreloadStatus, endTime: Long)
    
    /**
     * 删除旧任务
     */
    @Query("DELETE FROM image_preload_tasks WHERE createTime < :expireTime")
    suspend fun deleteOldTasks(expireTime: Long): Int
    
    /**
     * 删除指定任务
     */
    @Query("DELETE FROM image_preload_tasks WHERE taskId = :taskId")
    suspend fun deleteTask(taskId: String)
    
    /**
     * 清空所有任务
     */
    @Query("DELETE FROM image_preload_tasks")
    suspend fun clearAllTasks()
}