package com.wy.diary

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.wy.diary.data.cache.CacheManager
import com.wy.diary.data.cache.HybridImageCacheManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DiaryApplication : Application() {
    
    @Inject
    lateinit var cacheManager: CacheManager
    
    @Inject
    lateinit var imageCacheManager: HybridImageCacheManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化数据缓存管理器
        cacheManager.initialize()
        cacheManager.startPeriodicCacheSync()
        cacheManager.startPeriodicCacheCleanup()
        
        // 初始化图片缓存（异步执行）
        Thread {
            try {
                kotlinx.coroutines.runBlocking {
                    imageCacheManager.warmupCache(this@DiaryApplication)
                }
            } catch (e: Exception) {
                android.util.Log.e("DiaryApplication", "图片缓存预热失败", e)
            }
        }.start()
    }
}