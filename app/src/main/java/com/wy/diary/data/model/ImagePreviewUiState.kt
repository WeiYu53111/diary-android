package com.wy.diary.data.model

/**
 * 图片预览界面的UI状态
 */
data class ImagePreviewUiState(
    val imageUrls: List<String> = emptyList(),
    val currentPosition: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)