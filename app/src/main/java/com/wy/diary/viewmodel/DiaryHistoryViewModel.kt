package com.wy.diary.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.api.DiaryService
import com.wy.diary.api.RetrofitClient
import com.wy.diary.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// 日记项数据模型
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
)

// 用户信息数据模型
data class UserInfo(
    val diaryCount: Int,
    val registerTime: String
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

open class DiaryHistoryViewModel : ViewModel() {
    // 状态管理
    protected val _uiState = MutableStateFlow(DiaryHistoryUIState())
    val uiState: StateFlow<DiaryHistoryUIState> = _uiState.asStateFlow()
    
    // 分页管理
    private var currentPage = 1
    private val pageSize = 10
    private var totalCount = 0
    private var totalPages = 0
    
    // API 服务
    private val diaryService = RetrofitClient.createService(DiaryService::class.java)
    
    init {
        // 初始化时加载用户信息和日记列表
        loadUserInfo()
        loadDiaryList(true)
    }

    fun loadUserInfo() {
        // 从 API 获取用户信息
        // 这里暂时使用模拟数据
        _uiState.value = _uiState.value.copy(
            userInfo = UserInfo(0, "注册于 2025-05-13")
        )
    }
    
    fun loadDiaryList(isRefresh: Boolean) {
        // 避免重复加载
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            try {
                // 更新加载状态
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isRefreshing = isRefresh
                )
                
                // 重置页码（如果是刷新）
                if (isRefresh) {
                    currentPage = 1
                }
                
                // 从服务器获取数据
                val response = diaryService.getDiaryList(currentPage, pageSize)
                
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    
                    if (apiResponse.isSuccess() && apiResponse.data != null) {
                        val data = apiResponse.data!!
                        
                        // 更新分页信息
                        totalCount = data.totalCount
                        totalPages = data.totalPages
                        val hasMore = data.hasNext
                        
                        // 处理日记记录
                        val newDiaries = data.records.map { record ->
                            // 处理内容预览
                            val content = record.editorContent ?: ""
                            val contentPreview = content.replace(Regex("<[^>]+>"), "")
                                .let { if (it.length > 100) it.substring(0, 100) + "..." else it }
                            
                            // 创建 DiaryItem 对象
                            DiaryItem(
                                diaryId = record.diaryId,
                                logTime = record.logTime ?: "",
                                logWeek = record.logWeek ?: "",
                                logLunar = record.logLunar,
                                contentPreview = contentPreview,
                                content = content,
                                images = record.imageUrls?.map { imageUrl ->
                                    ImageUtils.processImageUrl(imageUrl)
                                }?: emptyList(),
                                createTimeFormatted = formatCreateTime(record.createTime),
                                address = record.address
                            )
                        }.sortedByDescending {
                            // 按日期倒序排列，最新的在前
                            it.createTimeFormatted
                        }
                        
                        // 更新状态
                        _uiState.value = if (isRefresh) {
                            // 刷新：替换所有数据
                            _uiState.value.copy(
                                diaryItems = newDiaries,
                                hasMoreData = hasMore,
                                userInfo = _uiState.value.userInfo.copy(
                                    diaryCount = totalCount
                                )
                            )
                        } else {
                            // 加载更多：添加到现有数据
                            _uiState.value.copy(
                                diaryItems = _uiState.value.diaryItems + newDiaries,
                                hasMoreData = hasMore
                            )
                        }
                        
                        // 如果不是刷新，增加页码
                        if (!isRefresh) {
                            currentPage++
                        }
                    } else {
                        // API 返回失败
                        _uiState.value = _uiState.value.copy(
                            errorMessage = apiResponse.message
                        )
                    }
                } else {
                    // HTTP 请求失败
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "网络请求失败: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                // 捕获异常
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    errorMessage = "网络错误: ${e.message ?: "请重试"}"
                )
            } finally {
                // 结束加载状态
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }
    
    // 日期格式化处理
    private fun formatCreateTime(createTime: String?): String {
        if (createTime == null) return "未知时间"

        return try {
            // 专门处理 "Tue May 13 20:59:40 UTC 2025" 格式
            val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            val date = dateFormat.parse(createTime)

            if (date != null) {
                // 转换为本地时区显示
                val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                outputFormat.format(date)
            } else {
                "日期格式错误"
            }
        } catch (e: Exception) {
            Log.e("DateParsing", "日期处理发生异常: ${e.message}")
            "日期格式错误"
        }
    }
    
    // 删除日记
    fun deleteDiary(diary: DiaryItem) {
        viewModelScope.launch {
            try {
                // 这里应该调用 API 删除日记
                // 模拟删除成功
                val updatedList = _uiState.value.diaryItems.filter { it.diaryId != diary.diaryId }
                totalCount -= 1
                
                _uiState.value = _uiState.value.copy(
                    diaryItems = updatedList,
                    userInfo = _uiState.value.userInfo.copy(
                        diaryCount = totalCount
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除失败: ${e.message ?: "请重试"}"
                )
            }
        }
    }
    
    // 重置错误信息
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}