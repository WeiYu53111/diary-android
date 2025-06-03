package com.wy.diary

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DiaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}