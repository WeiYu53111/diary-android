package com.wy.diary

import android.app.Application
import com.wy.diary.data.cache.CacheManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DiaryApplication : Application() {
    
    @Inject
    lateinit var cacheManager: CacheManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 启动缓存管理
        cacheManager.startPeriodicCacheSync()
        cacheManager.startPeriodicCacheCleanup()
    }
}