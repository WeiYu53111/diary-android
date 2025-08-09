package com.wy.diary.data.cache

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.wy.diary.data.local.ImageCacheDao
import com.wy.diary.data.local.ImageCacheEntity
import com.wy.diary.data.local.ImagePreloadTaskDao
import com.wy.diary.data.local.ImagePreloadTaskEntity
import com.wy.diary.data.local.PreloadStatus
import com.wy.diary.util.ImageLoaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图片预加载服务
 */
@Singleton
class ImagePreloadService @Inject constructor(
    private val imageCacheDao: ImageCacheDao,
    private val preloadTaskDao: ImagePreloadTaskDao
) {

    companion object {
        private const val TAG = "ImagePreloadService"
        private const val MAX_CONCURRENT_DOWNLOADS = 3 // 最大并发下载数
    }

    /**
     * 预加载图片列表
     * @param context 上下文
     * @param imageUrls 图片URL列表
     * @param priority 预加载优先级
     * @return 任务ID
     */
    suspend fun preloadImages(
        context: Context,
        imageUrls: List<String>,
        priority: PreloadPriority = PreloadPriority.NORMAL
    ): String {
        if (imageUrls.isEmpty()) {
            Log.w(TAG, "预加载图片列表为空")
            return ""
        }

        val taskId = UUID.randomUUID().toString()
        Log.d(TAG, "开始预加载任务: $taskId, 图片数量: ${imageUrls.size}")

        // 创建预加载任务
        val task = ImagePreloadTaskEntity(
            taskId = taskId,
            imageUrls = imageUrls.joinToString(","),
            status = PreloadStatus.PENDING,
            totalImages = imageUrls.size
        )
        preloadTaskDao.insertTask(task)

        // 执行预加载
        executePreloadTask(context, taskId, imageUrls, priority)

        return taskId
    }

    /**
     * 执行预加载任务
     */
    private suspend fun executePreloadTask(
        context: Context,
        taskId: String,
        imageUrls: List<String>,
        priority: PreloadPriority
    ) = withContext(Dispatchers.IO) {
        try {
            // 更新任务状态为运行中
            preloadTaskDao.updateTaskStatus(
                taskId = taskId,
                status = PreloadStatus.RUNNING,
                startTime = System.currentTimeMillis()
            )

            var completedCount = 0
            val totalCount = imageUrls.size

            // 分批处理图片，避免过度并发
            imageUrls.chunked(MAX_CONCURRENT_DOWNLOADS).forEach { batch ->
                coroutineScope {
                    val jobs = batch.map { imageUrl ->
                        async {
                            preloadSingleImage(context, imageUrl, priority)
                        }
                    }

                    // 等待当前批次完成
                    val results = jobs.awaitAll()
                    completedCount += results.count { it }

                    // 更新进度
                    val progress = (completedCount * 100) / totalCount
                    preloadTaskDao.updateTaskProgress(
                        taskId = taskId,
                        progress = progress,
                        completedImages = completedCount
                    )

                    Log.d(TAG, "预加载进度: $completedCount/$totalCount ($progress%)")
                }
            }

            // 完成任务
            preloadTaskDao.completeTask(
                taskId = taskId,
                status = PreloadStatus.COMPLETED,
                endTime = System.currentTimeMillis()
            )

            Log.d(TAG, "预加载任务完成: $taskId, 成功: $completedCount/$totalCount")

        } catch (e: Exception) {
            Log.e(TAG, "预加载任务失败: $taskId", e)
            preloadTaskDao.completeTask(
                taskId = taskId,
                status = PreloadStatus.FAILED,
                endTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * 预加载单张图片
     */
    private suspend fun preloadSingleImage(
        context: Context,
        imageUrl: String,
        priority: PreloadPriority
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "预加载图片: $imageUrl")

            // 检查是否已经缓存
            val cachedImage = imageCacheDao.getCachedImageUrl(imageUrl)
            if (cachedImage?.isPreloaded == true) {
                Log.d(TAG, "图片已预加载: $imageUrl")
                return@withContext true
            }

            // 使用Coil预加载
            val imageLoader = ImageLoaderFactory.getInstance(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()

            val result = imageLoader.execute(request)
            
            if (result is SuccessResult) {
                // 同时使用Glide预加载以确保兼容性
                try {
                    Glide.with(context)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .preload()
                } catch (e: Exception) {
                    Log.w(TAG, "Glide预加载失败: $imageUrl", e)
                }

                // 更新缓存状态
                val imageCache = cachedImage ?: ImageCacheEntity(
                    originalUrl = imageUrl,
                    processedUrl = imageUrl
                )
                
                imageCacheDao.insertImageUrl(
                    imageCache.copy(
                        isPreloaded = true,
                        lastAccessTime = System.currentTimeMillis()
                    )
                )

                Log.d(TAG, "图片预加载成功: $imageUrl")
                return@withContext true
            } else {
                Log.w(TAG, "图片预加载失败: $imageUrl")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "预加载图片异常: $imageUrl", e)
            return@withContext false
        }
    }

    /**
     * 取消预加载任务
     */
    suspend fun cancelPreloadTask(taskId: String) {
        Log.d(TAG, "取消预加载任务: $taskId")
        preloadTaskDao.completeTask(
            taskId = taskId,
            status = PreloadStatus.CANCELLED,
            endTime = System.currentTimeMillis()
        )
    }

    /**
     * 获取预加载任务状态
     */
    suspend fun getPreloadTaskStatus(taskId: String): ImagePreloadTaskEntity? {
        return preloadTaskDao.getTaskById(taskId)
    }

    /**
     * 清理已完成的旧任务
     */
    suspend fun cleanupOldTasks(daysOld: Int = 7) {
        val expireTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        val deletedCount = preloadTaskDao.deleteOldTasks(expireTime)
        Log.d(TAG, "清理旧预加载任务: $deletedCount 个")
    }

    /**
     * 检查图片是否已预加载
     */
    suspend fun isImagePreloaded(imageUrl: String): Boolean {
        val cachedImage = imageCacheDao.getCachedImageUrl(imageUrl)
        return cachedImage?.isPreloaded == true
    }
}

/**
 * 预加载优先级
 */
enum class PreloadPriority {
    LOW,        // 低优先级 - 在空闲时预加载
    NORMAL,     // 普通优先级 - 正常预加载
    HIGH        // 高优先级 - 立即预加载
}