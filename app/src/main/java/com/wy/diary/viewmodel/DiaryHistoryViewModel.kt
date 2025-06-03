package com.wy.diary.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.data.model.DiaryHistoryUIState
import com.wy.diary.data.model.DiaryItem
import com.wy.diary.data.model.DiaryRecord
import com.wy.diary.data.model.UserInfo
import com.wy.diary.data.remote.RetrofitClient
import com.wy.diary.data.repository.AppRepository
import com.wy.diary.data.repository.DiaryRepository
import com.wy.diary.data.repository.UserInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject


@HiltViewModel
open class DiaryHistoryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val userInfoRepository: UserInfoRepository,
) : ViewModel() {
    // 状态管理
    protected val _uiState = MutableStateFlow(DiaryHistoryUIState())
    val uiState: StateFlow<DiaryHistoryUIState> = _uiState.asStateFlow()

    // 分页管理
    private var currentPage = 1
    private val pageSize = 10
    private var totalCount = 0
    private var totalPages = 0

    init {
        // 初始化时加载用户信息和日记列表
        loadUserInfo()
        loadDiaryList(true)
    }

    fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val result = userInfoRepository.getUserInfo()
                
                result.fold(
                    onSuccess = { userInfo ->
                        _uiState.value = _uiState.value.copy(
                            userInfo = UserInfo(
                                diaryCount = userInfo.diaryCount,
                                registerTime = "注册于 ${userInfo.registerTime}"
                            )
                        )
                    },
                    onFailure = { error ->
                        Log.e("DiaryHistoryViewModel", "加载用户信息失败", error)
                        // 使用默认用户信息
                        _uiState.value = _uiState.value.copy(
                            userInfo = UserInfo(0, "注册于 2025-05-13")
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("DiaryHistoryViewModel", "加载用户信息异常", e)
                // 使用默认用户信息
                _uiState.value = _uiState.value.copy(
                    userInfo = UserInfo(0, "注册于 2025-05-13")
                )
            }
        }
    }

    fun loadDiaryList(isRefresh: Boolean) {
        // 避免重复加载
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            // 更新加载状态
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isRefreshing = isRefresh
            )

            // 重置页码（如果是刷新）
            if (isRefresh) {
                currentPage = 1
            }

            // 从服务器获取数据 - Repository 返回 Result 类型
            val result = diaryRepository.getDiaryList(currentPage, pageSize)
            
            result.fold(
                onSuccess = { data ->
                    // 更新分页信息
                    totalCount = data.totalCount
                    totalPages = data.totalPages
                    val hasMore = data.hasNext

                    // 处理日记记录
                    val newDiaries = data.records?.map { record ->
                        mapToDiaryItem(record)
                    }?.sortedByDescending {
                        // 按日期倒序排列，最新的在前
                        it.createTimeFormatted
                    } ?: emptyList()

                    // 更新状态
                    _uiState.value = if (isRefresh) {
                        handleRefreshSuccess(newDiaries, hasMore)
                    } else {
                        handleLoadMoreSuccess(newDiaries, hasMore)
                    }

                    // 如果不是刷新，增加页码
                    if (!isRefresh && newDiaries.isNotEmpty()) {
                        currentPage++
                    }
                },
                onFailure = { error ->
                    handleLoadFailure(error)
                }
            )

            // 无论成功还是失败，都结束加载状态
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false
            )
        }
    }
    
    private fun mapToDiaryItem(record: DiaryRecord): DiaryItem {
        // 处理内容预览
        val content = record.editorContent ?: ""
        val contentPreview = content.replace(Regex("<[^>]+>"), "")
            .let { if (it.length > 100) it.substring(0, 100) + "..." else it }

        // 创建 DiaryItem 对象
        return DiaryItem(
            diaryId = record.diaryId,
            logTime = record.logTime ?: "",
            logWeek = record.logWeek ?: "",
            logLunar = record.logLunar,
            contentPreview = contentPreview,
            content = content,
            images = record.imageUrls?: emptyList(),
            createTimeFormatted = formatCreateTime(record.createTime),
            address = record.address
        )
    }
    
    private fun handleRefreshSuccess(newDiaries: List<DiaryItem>, hasMore: Boolean): DiaryHistoryUIState {
        return _uiState.value.copy(
            diaryItems = newDiaries,
            hasMoreData = hasMore,
            userInfo = _uiState.value.userInfo.copy(
                diaryCount = totalCount
            ),
            errorMessage = if (newDiaries.isEmpty() && totalCount == 0) "暂无数据" else null
        )
    }
    
    private fun handleLoadMoreSuccess(newDiaries: List<DiaryItem>, hasMore: Boolean): DiaryHistoryUIState {
        return _uiState.value.copy(
            diaryItems = _uiState.value.diaryItems + newDiaries,
            hasMoreData = hasMore,
            errorMessage = if (newDiaries.isEmpty() && !hasMore) "没有更多数据了" else null
        )
    }
    
    private fun handleLoadFailure(error: Throwable): DiaryHistoryUIState {
        Log.e("DiaryHistoryViewModel", "加载日记列表失败", error)
        return _uiState.value.copy(
            errorMessage = "加载失败: ${error.message ?: "网络错误，请稍后重试"}"
        )
    }

    // 日期格式化处理
    private fun formatCreateTime(createTime: String?): String {
        if (createTime == null) return "未知时间"

        return try {
            // 专门处理 "Tue May 13 20:59:40 UTC 2025" 格式
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = dateFormat.parse(createTime)

            if (date != null) {
                // 转换为本地时区显示
                val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                outputFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
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
            _uiState.value = _uiState.value.copy(
                isLoading = true
            )
            
            try {
                // 调用仓库层删除日记的方法
                val result = diaryRepository.deleteDiary(diary.diaryId,diary.createTimeFormatted)
                
                result.fold(
                    onSuccess = { 
                        // 删除成功，更新本地数据
                        val updatedList = _uiState.value.diaryItems.filter { it.diaryId != diary.diaryId }
                        totalCount -= 1

                        _uiState.value = _uiState.value.copy(
                            diaryItems = updatedList,
                            userInfo = _uiState.value.userInfo.copy(
                                diaryCount = totalCount
                            )
                        )
                    },
                    onFailure = { error ->
                        // 删除失败，显示错误信息
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "删除失败: ${error.message ?: "请重试"}"
                        )
                    }
                )
            } catch (e: Exception) {
                // 捕获其他可能的异常
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除操作异常: ${e.message ?: "请重试"}"
                )
            } finally {
                // 无论成功还是失败，都结束加载状态
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }
        }
    }

    // 重置错误信息
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

}