package com.wy.diary.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * 自定义Glide模块 - 增强图片缓存配置
 */
@GlideModule
class DiaryGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 配置内存缓存 - 使用当前可用内存的1/4，最少40MB，最多250MB
        val memoryCacheSizeBytes = 1024 * 1024 * 100 // 100MB
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))
        
        // 配置磁盘缓存 - 500MB
        val diskCacheSizeBytes = 1024 * 1024 * 500 // 500MB
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(
                context, 
                "diary_image_cache", 
                diskCacheSizeBytes.toLong()
            )
        )
        
        // 设置默认请求选项
        val requestOptions = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565) // 节省内存
            .disallowHardwareConfig() // 避免一些兼容性问题
            
        builder.setDefaultRequestOptions(requestOptions)
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // 配置OkHttp客户端，支持更长的超时时间
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }

    override fun isManifestParsingEnabled(): Boolean {
        // 禁用清单解析以提升启动速度
        return false
    }
}