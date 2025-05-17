package com.wy.diary.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

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

open class DiaryDetailViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    protected val _uiState = MutableStateFlow(DiaryDetailUiState())
    var uiState: StateFlow<DiaryDetailUiState> = _uiState.asStateFlow()

    init {
        loadDiaryDetail()
    }

    fun loadDiaryDetail() {
        val diaryId = savedStateHandle.get<String>("diaryId") ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // 如果我们已经有了传入的数据，就直接使用
                if (savedStateHandle.contains("logTime")) {
                    // 使用传入的数据创建一个DiaryDetailItem
                    val detailData = DiaryDetailItem(
                        diaryId = diaryId,
                        logTime = savedStateHandle.get<String>("logTime") ?: "",
                        logWeek = savedStateHandle.get<String>("logWeek") ?: "",
                        logLunar = savedStateHandle.get<String>("logLunar") ?: "",
                        content = savedStateHandle.get<String>("content") ?: "",
                        images = savedStateHandle.get<List<String>>("images") ?: emptyList(),
                        createTimeFormatted = savedStateHandle.get<String>("createTime") ?: "",
                        address = savedStateHandle.get<String>("address") ?: ""
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        diary = detailData,
                        isLoading = false
                    )
                } else {
                    // 如果没有传入完整数据，则调用API获取
                    val detailData = fetchDiaryDetail(diaryId)
                    
                    _uiState.value = _uiState.value.copy(
                        diary = detailData,
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "网络错误，请检查您的网络连接",
                    isLoading = false
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "服务器错误: ${e.message()}",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "未知错误: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    private suspend fun fetchDiaryDetail(diaryId: String): DiaryDetailItem {
        // TODO: 替换为实际API调用
        // 模拟网络请求延迟
        kotlinx.coroutines.delay(800)
        
        // 返回模拟数据
        return DiaryDetailItem(
            diaryId = diaryId,
            logTime = "2025年5月15日",
            logWeek = "星期四",
            logLunar = "四月初八",
            content = "今天天气不错，和朋友一起去公园散步。看到许多人在户外活动，感觉春天真的来了。\n\n" +
                "下午去了图书馆，借了几本很久想看的书。在那里安静地度过了几个小时，感觉很充实。\n\n" +
                "晚上和家人一起吃了饭，聊了很多最近发生的事情。总的来说，这是很平静但很满足的一天。",
            images = listOf(
                "https://picsum.photos/id/237/800/600",
                "https://picsum.photos/id/238/800/600",
                "https://picsum.photos/id/239/800/600"
            ),
            createTimeFormatted = "2025-05-15 10:30",
            address = "北京市海淀区"
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}