package com.wy.diary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 图片URL缓存映射实体
 * 用于缓存处理后的完整图片URL，避免重复计算
 */
@Entity(tableName = "image_cache_urls")
data class ImageCacheEntity(
    @PrimaryKey
    val originalUrl: String,         // 原始图片路径 (如: "path/to/image.jpg")
    val processedUrl: String,        // 处理后的完整URL (如: "http://api.../image?id=xxx&file=...")
    val lastAccessTime: Long = System.currentTimeMillis(),  // 最后访问时间
    val cacheTime: Long = System.currentTimeMillis(),       // 缓存时间
    val fileSize: Long = 0,          // 文件大小(字节)
    val isPreloaded: Boolean = false // 是否已预加载到磁盘
)

/**
 * 图片预加载任务实体
 * 用于管理后台预加载任务
 */
@Entity(tableName = "image_preload_tasks")
data class ImagePreloadTaskEntity(
    @PrimaryKey
    val taskId: String,              // 任务ID
    val imageUrls: String,           // JSON格式的图片URL列表
    val status: PreloadStatus = PreloadStatus.PENDING,    // 预加载状态
    val createTime: Long = System.currentTimeMillis(),    // 创建时间
    val startTime: Long? = null,     // 开始时间
    val endTime: Long? = null,       // 结束时间
    val progress: Int = 0,           // 进度百分比 (0-100)
    val totalImages: Int = 0,        // 总图片数量
    val completedImages: Int = 0     // 已完成图片数量
)

/**
 * 预加载状态枚举
 */
enum class PreloadStatus {
    PENDING,    // 等待中
    RUNNING,    // 运行中
    COMPLETED,  // 已完成
    FAILED,     // 失败
    CANCELLED   // 已取消
}