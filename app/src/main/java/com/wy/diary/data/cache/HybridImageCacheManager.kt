package com.wy.diary.data.cache

import android.content.Context
import android.util.Log
import com.wy.diary.data.local.ImageCacheDao
import com.wy.diary.data.local.ImageCacheEntity
import com.wy.diary.data.repository.AppRepository
import com.wy.diary.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 混合图片缓存管理器
 * 方案C的核心实现 - 整合URL处理、缓存管理、预加载功能
 */
@Singleton
class HybridImageCacheManager @Inject constructor(
    private val imageCacheDao: ImageCacheDao,
    private val imagePreloadService: ImagePreloadService,
    private val appRepository: AppRepository
) {

    companion object {
        private const val TAG = "HybridImageCacheManager"
        private const val URL_CACHE_EXPIRE_DAYS = 7 // URL缓存过期天数
        private const val MAX_CACHE_SIZE_MB = 500 // 最大缓存大小
    }

    /**
     * 处理图片URL - 核心方法
     * 将原始图片路径转换为完整的可访问URL，并缓存映射关系
     */
    suspend fun processImageUrl(originalUrl: String): String = withContext(Dispatchers.IO) {
        if (originalUrl.startsWith("http://") || originalUrl.startsWith("https://")) {
            // 已经是完整URL，直接返回
            return@withContext originalUrl
        }

        try {
            // 1. 先查询缓存
            val cachedUrl = imageCacheDao.getCachedImageUrl(originalUrl)
            if (cachedUrl != null) {
                // 更新访问时间
                imageCacheDao.updateLastAccessTime(originalUrl, System.currentTimeMillis())
                Log.d(TAG, "从缓存获取图片URL: $originalUrl -> ${cachedUrl.processedUrl}")
                return@withContext cachedUrl.processedUrl
            }

            // 2. 生成完整URL
            val openid = appRepository.getToken()
            val processedUrl = "${RetrofitClient.BASE_URL}${RetrofitClient.IMAGE_API_PREFX}?id=${openid}&file=${originalUrl}"

            // 3. 缓存URL映射
            val imageCache = ImageCacheEntity(
                originalUrl = originalUrl,
                processedUrl = processedUrl,
                lastAccessTime = System.currentTimeMillis(),
                cacheTime = System.currentTimeMillis()
            )
            imageCacheDao.insertImageUrl(imageCache)

            Log.d(TAG, "生成并缓存图片URL: $originalUrl -> $processedUrl")
            return@withContext processedUrl

        } catch (e: Exception) {
            Log.e(TAG, "处理图片URL失败: $originalUrl", e)
            return@withContext originalUrl
        }
    }

    /**
     * 批量处理图片URL列表
     */
    suspend fun processImageUrls(originalUrls: List<String>): List<String> = withContext(Dispatchers.IO) {
        if (originalUrls.isEmpty()) return@withContext emptyList()

        return@withContext originalUrls.map { processImageUrl(it) }
    }

    /**
     * 预加载图片列表
     * @param context 上下文
     * @param originalUrls 原始图片URL列表
     * @param priority 预加载优先级
     */
    suspend fun preloadImages(
        context: Context,
        originalUrls: List<String>,
        priority: PreloadPriority = PreloadPriority.NORMAL
    ): String {
        if (originalUrls.isEmpty()) return ""

        // 1. 处理URL
        val processedUrls = processImageUrls(originalUrls)
        
        // 2. 过滤已预加载的图片
        val urlsToPreload = mutableListOf<String>()
        processedUrls.forEach { url ->
            if (!imagePreloadService.isImagePreloaded(url)) {
                urlsToPreload.add(url)
            }
        }

        if (urlsToPreload.isEmpty()) {
            Log.d(TAG, "所有图片已预加载，跳过")
            return ""
        }

        Log.d(TAG, "开始预加载 ${urlsToPreload.size} 张图片")

        // 3. 执行预加载
        return imagePreloadService.preloadImages(context, urlsToPreload, priority)
    }

    /**
     * 智能预加载 - 根据用户行为预加载相关图片
     * @param context 上下文  
     * @param currentImageUrls 当前查看的图片列表
     * @param relatedImageUrls 相关图片列表（如相邻日记的图片）
     */
    suspend fun smartPreload(
        context: Context,
        currentImageUrls: List<String>,
        relatedImageUrls: List<String> = emptyList()
    ) {
        // 1. 高优先级预加载当前图片
        if (currentImageUrls.isNotEmpty()) {
            preloadImages(context, currentImageUrls, PreloadPriority.HIGH)
        }

        // 2. 普通优先级预加载相关图片
        if (relatedImageUrls.isNotEmpty()) {
            preloadImages(context, relatedImageUrls, PreloadPriority.NORMAL)
        }
    }

    /**
     * 检查图片是否已缓存
     */
    suspend fun isImageCached(originalUrl: String): Boolean {
        val processedUrl = processImageUrl(originalUrl)
        return imagePreloadService.isImagePreloaded(processedUrl)
    }

    /**
     * 获取缓存统计信息
     */
    suspend fun getCacheStats(): ImageCacheStats = withContext(Dispatchers.IO) {
        val urlCount = imageCacheDao.getCacheCount()
        val totalSize = imageCacheDao.getTotalCacheSize() ?: 0L
        val preloadedImages = imageCacheDao.getPreloadedImages()
        
        ImageCacheStats(
            urlCacheCount = urlCount,
            totalCacheSize = totalSize,
            preloadedImageCount = preloadedImages.size,
            lastCleanupTime = System.currentTimeMillis()
        )
    }

    /**
     * 清理过期缓存
     */
    suspend fun cleanupExpiredCache() = withContext(Dispatchers.IO) {
        try {
            // 清理过期的URL缓存
            val expireTime = System.currentTimeMillis() - (URL_CACHE_EXPIRE_DAYS * 24 * 60 * 60 * 1000L)
            val deletedUrls = imageCacheDao.deleteExpiredUrls(expireTime)
            
            // 清理旧的预加载任务
            imagePreloadService.cleanupOldTasks()
            
            Log.d(TAG, "缓存清理完成: 删除 $deletedUrls 个过期URL缓存")
            
        } catch (e: Exception) {
            Log.e(TAG, "缓存清理失败", e)
        }
    }

    /**
     * 清空所有缓存
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            imageCacheDao.clearAllCache()
            Log.d(TAG, "已清空所有图片缓存")
        } catch (e: Exception) {
            Log.e(TAG, "清空缓存失败", e)
        }
    }

    /**
     * 预热缓存 - 应用启动时调用
     */
    suspend fun warmupCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始预热图片缓存")
            
            // 获取最近访问的图片
            val recentImages = imageCacheDao.getPreloadedImages().take(10)
            if (recentImages.isNotEmpty()) {
                val recentUrls = recentImages.map { it.processedUrl }
                imagePreloadService.preloadImages(context, recentUrls, PreloadPriority.LOW)
                Log.d(TAG, "预热缓存: ${recentImages.size} 张最近图片")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "预热缓存失败", e)
        }
    }
}

/**
 * 图片缓存统计信息
 */
data class ImageCacheStats(
    val urlCacheCount: Int,        // URL缓存数量
    val totalCacheSize: Long,      // 总缓存大小(字节)
    val preloadedImageCount: Int,  // 预加载图片数量
    val lastCleanupTime: Long      // 最后清理时间
) {
    val totalCacheSizeMB: Double
        get() = totalCacheSize / (1024.0 * 1024.0)
}