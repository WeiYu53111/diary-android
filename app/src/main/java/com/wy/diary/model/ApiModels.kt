package com.wy.diary.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val status: String,
    val message: String,
    val data: T?
) {
    // 添加辅助方法
    fun isSuccess(): Boolean = status == "success"
}

data class DiaryIdResponse(
    val diaryId: String
)

data class ImageUploadResponse(
    val url: String?
)

data class DiaryRequest(
    val openId: String,
    val editorContent: String,
    val createTime: String,
    val logTime: String,
    val logWeek: String,
    val logLunar: String,
    val address: String,
    val imageUrls: List<String>,
    val diaryId: String
)

data class DiaryListPagedData(
    @SerializedName("records") val records: List<DiaryRecord>,
    @SerializedName("totalCount") val totalCount: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("hasNext") val hasNext: Boolean
)

data class DiaryRecord(
    @SerializedName("diaryId") val diaryId: String,
    @SerializedName("logTime") val logTime: String?,
    @SerializedName("logWeek") val logWeek: String?,
    @SerializedName("logLunar") val logLunar: String?,
    @SerializedName("editorContent") val editorContent: String?,
    @SerializedName("imageUrls") val imageUrls: List<String>?,
    @SerializedName("createTime") val createTime: String?,
    @SerializedName("address") val address: String?
)

data class BackupStatusResponse(
    val status: String,
    val taskId: String
)

data class BackupStartResponse(
    val taskId: String
)