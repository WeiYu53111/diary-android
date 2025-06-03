package com.wy.diary.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit

@GlideModule
class MyGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // 创建自定义的 OkHttpClient，设置更长的超时时间
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // 连接超时时间
            .readTimeout(30, TimeUnit.SECONDS)    // 读取超时时间
            .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时时间
            .build()

        // 替换 Glide 的默认 HttpUrlConnection
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }
}