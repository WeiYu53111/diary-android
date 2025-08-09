package com.wy.diary.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Coil图片加载器工厂 - 创建配置优化的ImageLoader
 */
object ImageLoaderFactory {

    @Volatile
    private var INSTANCE: ImageLoader? = null

    fun getInstance(context: Context): ImageLoader {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: createImageLoader(context).also { INSTANCE = it }
        }
    }

    private fun createImageLoader(context: Context): ImageLoader {
        // 配置OkHttp客户端
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            // 内存缓存配置 - 使用可用内存的25%，最少64MB，最多256MB
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            // 磁盘缓存配置 - 500MB
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(500 * 1024 * 1024) // 500MB
                    .build()
            }
            // 默认缓存策略
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // 启用日志（仅在DEBUG模式）
            .logger(if (android.util.Log.isLoggable("Coil", android.util.Log.DEBUG)) {
                coil.util.DebugLogger()
            } else null)
            // 启用网络观察者
            .networkObserverEnabled(true)
            // 尊重缓存头
            .respectCacheHeaders(false) // 我们自己管理缓存
            .build()
    }

    /**
     * 清理ImageLoader实例（用于测试或重新配置）
     */
    fun reset() {
        INSTANCE?.shutdown()
        INSTANCE = null
    }
}