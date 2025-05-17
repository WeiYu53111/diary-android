package com.wy.diary

import android.app.Application
import com.wy.diary.api.TokenManager

class DiaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 TokenManager，提供应用级别的 Context
        TokenManager.init(applicationContext)
    }
}