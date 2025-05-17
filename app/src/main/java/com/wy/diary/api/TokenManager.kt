package com.wy.diary.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object TokenManager {
    // 存储应用程序上下文
    private var applicationContext: Context? = null
    private var cachedToken: String? = null
    private var sharedPreferences: SharedPreferences? = null
    
    /**
     * 初始化 TokenManager
     * @param context 应用程序上下文
     */
    fun init(context: Context) {
        this.applicationContext = context.applicationContext
    }

    /**
     * 保存新的令牌到 SharedPreferences
     * @param newToken 新的令牌字符串
     */
    fun saveToken(newToken: String) {
        cachedToken = newToken
        try {
            getSharedPreferences().edit().putString("auth_token", newToken).apply()
            Log.d("TokenManager", "令牌已保存")
        } catch (e: Exception) {
            Log.e("TokenManager", "保存令牌失败", e)
        }
    }

    /**
     * 从 SharedPreferences 获取令牌
     * @return 令牌字符串，如果没有则返回空字符串
     */
    fun getToken(): String {
        // 如果已经有缓存的有效令牌，直接返回
        if (!cachedToken.isNullOrEmpty()) {
            return cachedToken!!
        }

        // 检查是否已初始化
        if (applicationContext == null) {
            Log.e("TokenManager", "TokenManager未初始化，无法获取令牌")
            return ""
        }
        
        // 从 SharedPreferences 获取
        try {
            val token = getSharedPreferences().getString("auth_token", "") ?: ""
            if (token.isNotEmpty()) {
                //抛出错误  
                throw IllegalStateException("token为空")
                cachedToken = token // 只有当获取到的令牌不为空时才缓存
            }
            return token
        } catch (e: Exception) {
            Log.e("TokenManager", "获取令牌失败", e)
            return ""
        }
    }


    /**
     * 获取 SharedPreferences 实例
     * @throws IllegalStateException 如果TokenManager未初始化
     */
    private fun getSharedPreferences(): SharedPreferences {
        sharedPreferences?.let { return it }
        
        val context = applicationContext ?: throw IllegalStateException("TokenManager未初始化，请先调用init(context)方法")
        
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences = prefs
        return prefs
    }

}