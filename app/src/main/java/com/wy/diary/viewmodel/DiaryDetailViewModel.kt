package com.wy.diary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.data.model.DiaryDetailItem
import com.wy.diary.data.model.DiaryDetailUiState
import com.wy.diary.data.model.DiaryItem
import com.wy.diary.data.model.DiaryRecord
import com.wy.diary.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
open class DiaryDetailViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {
    
    // UI 状态
    private val _uiState = MutableStateFlow(DiaryDetailUiState(isLoading = true))
    val uiState: StateFlow<DiaryDetailUiState> = _uiState.asStateFlow()
    
    /**
     * 加载日记详情
     */
    fun loadDiary(diaryItem: DiaryItem) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        diary = mapToDiaryDetailItem(diaryItem),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "发生异常: ${e.message ?: "未知错误"}"
                    )
                }
            }
        }
    }
    
    /**
     * 将仓库返回的数据模型转换为UI数据模型
     */
    private fun mapToDiaryDetailItem(diaryItem: DiaryItem): DiaryDetailItem {
        return DiaryDetailItem(
            diaryId = diaryItem.diaryId,
            logTime = diaryItem.logTime ?: "",
            logWeek = diaryItem.logWeek ?: "",
            logLunar = diaryItem.logLunar ?: "",
            content = diaryItem.content ?: "",
            images = diaryItem.images,
            createTimeFormatted = diaryItem.createTimeFormatted,
            address = diaryItem.address ?: ""
        )
    }
    
    /**
     * 格式化创建时间
     */
    private fun formatCreateTime(createTime: String?): String {
        return try {
            if (createTime.isNullOrEmpty()) return ""
            
            // 假设返回的是 ISO 格式的时间字符串，如 "2023-05-03T14:30:45"
            val pattern = "yyyy-MM-dd'T'HH:mm:ss"
            val outputPattern = "yyyy-MM-dd HH:mm"
            
            val inputFormat = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat(outputPattern, java.util.Locale.getDefault())
            
            val date = inputFormat.parse(createTime)
            date?.let { outputFormat.format(it) } ?: ""
        } catch (e: Exception) {
            createTime ?: ""
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}