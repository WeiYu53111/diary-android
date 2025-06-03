package com.wy.diary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.data.model.ImagePreviewUiState
import com.wy.diary.data.repository.AppRepository
import com.wy.diary.data.remote.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 图片预览界面的ViewModel
 */
@HiltViewModel
class ImagePreviewViewModel @Inject constructor(private val appRepository: AppRepository) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(ImagePreviewUiState())
    val uiState: StateFlow<ImagePreviewUiState> = _uiState.asStateFlow()
    
    /**
     * 初始化图片列表和初始位置
     */
    fun initialize(imageUrls: List<String>, initialPosition: Int) {
        _uiState.update { it.copy(
            imageUrls = imageUrls,
            currentPosition = initialPosition
        )}
    }
    
    /**
     * 更新当前页位置
     */
    fun updateCurrentPosition(position: Int) {
        _uiState.update { it.copy(currentPosition = position) }
    }
}