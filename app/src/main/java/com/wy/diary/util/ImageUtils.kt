package com.wy.diary.util

import com.wy.diary.api.RetrofitClient
import com.wy.diary.api.TokenManager

object ImageUtils {

    // 处理图片URL
     fun processImageUrl(imageUrl: String): String {
        return if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            // 获取用户ID
            val openid = TokenManager.getToken()

            // 构建查询参数格式: ?id=openid&file=xxx
            "${RetrofitClient.BASE_URL}${RetrofitClient.IMAGE_API_PREFX}?id=${openid}&file=${imageUrl}"
        } else {
            imageUrl
        }
    }

}