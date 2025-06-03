package com.wy.diary.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.wy.diary.BuildConfig

object RetrofitClient {
    val BASE_URL = BuildConfig.API_BASE_URL
    const val IMAGE_API_PREFX = "/api/images/view"
    
    // 创建日志拦截器
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 创建一个拦截器，专门用于确认请求是否发送
    private val confirmationInterceptor = Interceptor { chain ->
        val request = chain.request()
        Log.d("NetworkConfirmation", "发送请求: ${request.url}")
        val response = chain.proceed(request)
        Log.d("NetworkConfirmation", "收到响应: ${response.code}")
        response
    }       
    
    // 暴露 OkHttpClient 创建方法，但不包括身份验证拦截器
    // AuthInterceptor 将通过 Hilt 注入
    fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(confirmationInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    // 保留单例 Retrofit 实例，用于非 Hilt 环境
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // 兼容旧代码的创建服务方法
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
