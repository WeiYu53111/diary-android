package com.wy.diary.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.wy.diary.BuildConfig

object RetrofitClient {
     val BASE_URL = BuildConfig.API_BASE_URL
     //const val BASE_URL = "http://115.190.79.225:7080";
     const val IMAGE_API_PREFX = "/api/images/view"
    // 创建日志拦截器
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // 显示详细信息，包括请求头和响应体
    }

    // 创建一个拦截器，专门用于确认请求是否发送
    private val confirmationInterceptor = Interceptor { chain ->
        val request = chain.request()
        Log.d("NetworkConfirmation", "发送请求: ${request.url}")
        val response = chain.proceed(request)
        Log.d("NetworkConfirmation", "收到响应: ${response.code}")
        response
    }       

    // 创建一个拦截器，在每个请求中添加 token
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // 从本地存储获取 token
        val token = TokenManager.getToken()
        
        // 只有当 token 不为空时才添加
        val newRequest = if (token.isNotEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token") // 通常使用 Bearer 格式
                .build()
        } else {
            originalRequest
        }
        
        chain.proceed(newRequest)
    }
    
    // 创建 OkHttpClient 并添加拦截器
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(confirmationInterceptor) // 添加确认拦截器
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)  // 添加日志拦截器
        .build()
    
    // 在 Retrofit 构建器中使用自定义的 OkHttpClient
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)  // 设置自定义 client
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
    
    // 添加一个方法来获取身份验证令牌，用于下载管理器等服务
    fun getAuthToken(): String {
        return TokenManager.getToken()
    }
}
