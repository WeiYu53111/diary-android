package com.wy.diary.data.model

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

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

// UI 状态数据类
data class DiaryHistoryUIState(
    val isLoading: Boolean = false,
    val diaryItems: List<DiaryItem> = emptyList(),
    val userInfo: UserInfo = UserInfo(0, "注册于 2025-05-13"),
    val hasMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)



// 日记项数据模型
@Parcelize
data class DiaryItem(
    val diaryId: String,
    val logTime: String,
    val logWeek: String,
    val logLunar: String?,
    val contentPreview: String,
    val content: String,
    val images: List<String> = emptyList(),
    val createTimeFormatted: String,
    val address: String?
): Parcelable

// 用户信息数据模型
data class UserInfo(
    val diaryCount: Int,
    val registerTime: String
)

/**
 * 登录界面UI状态
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val error: String? = null
)



data class DiaryDetailUiState(
    val isLoading: Boolean = true,
    val diary: DiaryDetailItem? = null,
    val errorMessage: String? = null
)

data class DiaryDetailItem(
    val diaryId: String,
    val logTime: String,
    val logWeek: String,
    val logLunar: String,
    val content: String,
    val images: List<String>,
    val createTimeFormatted: String,
    val address: String
)



/**
 * 日记UI状态 - 包含所有需要在UI层展示的状态
 */
data class DiaryUiState(
    val editorContent: String = "",
    val photos: List<Uri> = emptyList(),
    val address: String = "未选择地址",
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val savingStep: SavingStep = SavingStep.NONE,  // 添加这个字段
    val saveProgress: Float = 0f,                  // 添加这个字段
    val error: String? = null,
    val uploadProgress: Float = 0f, // 0-1之间
    val currentUploadingPhoto: Uri? = null
)

// 保存步骤枚举
enum class SavingStep {
    NONE,
    PREPARING,
    UPLOADING_IMAGES,
    SAVING_CONTENT,
    FINALIZING
}


data class DeleteDiaryRequest(
    val diaryId: String,
    val createYear: String
)