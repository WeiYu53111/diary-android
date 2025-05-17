package com.wy.diary.api

import com.wy.diary.model.ApiResponse
import retrofit2.Response

/**
 * 检查 API 响应是否成功（HTTP 成功 + success="success"）
 */
fun <T> Response<ApiResponse<T>>.isApiCallSuccess(): Boolean {
    return isSuccessful && body()?.isSuccess() == true
}

/**
 * 获取错误消息
 */
fun <T> Response<ApiResponse<T>>.getErrorMessage(): String {
    return body()?.message ?: message()
}

/**
 * 安全获取响应数据
 */
fun <T> Response<ApiResponse<T>>.getDataSafely(): T? {
    return if (isApiCallSuccess()) body()?.data else null
}