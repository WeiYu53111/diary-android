package com.wy.diary.data.remote

import com.wy.diary.data.repository.AppRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appRepository: AppRepository
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 使用 runBlocking 获取token (仅在拦截器中这样使用)
        val token = runBlocking { appRepository.getToken() }
        
        val newRequest = if (token.isNotEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}