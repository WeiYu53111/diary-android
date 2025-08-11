package com.wy.diary.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.data.model.DiaryHistoryUIState
import com.wy.diary.data.model.DiaryItem
import com.wy.diary.data.model.UserInfo
import com.wy.diary.data.repository.DiaryRepository
import com.wy.diary.data.repository.DiaryRepositoryWithCache
import com.wy.diary.data.repository.UserInfoRepository
import com.wy.diary.util.toDiaryItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 支持缓存的日记历史ViewModel
 * 这个ViewModel演示了如何同时使用缓存和网络数据
 */
@HiltViewModel
class DiaryHistoryViewModelWithCache @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val userInfoRepository: UserInfoRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DiaryHistoryViewModelWithCache"
        private const val PAGE_SIZE = 10
    }

    // UI状态管理
    private val _uiState = MutableStateFlow(DiaryHistoryUIState())
    val uiState: StateFlow<DiaryHistoryUIState> = _uiState.asStateFlow()

    // 分页管理
    private var currentPage = 1
    private var totalPages = 0
    private var isLoadingMore = false

    // 缓存Repository引用（用于访问缓存特定功能）
    private val cacheRepository = diaryRepository as? DiaryRepositoryWithCache

    init {
        Log.d(TAG, "初始化ViewModel")
        loadUserInfo()
        setupCacheListener()
        loadDiaryList(true)
    }

    /**
     * 设置缓存数据监听
     * 当本地缓存数据发生变化时，自动更新UI（仅在初始加载时）
     */
    private fun setupCacheListener() {
        cacheRepository?.getCachedDiariesFlow()
            ?.combine(_uiState) { cachedRecords, currentState ->
                // 只在初始状态（没有数据且不在加载中）时使用缓存数据初始化UI
                if (cachedRecords.isNotEmpty() && 
                    currentState.diaryItems.isEmpty() && 
                    !currentState.isLoading && 
                    !currentState.isRefreshing) {
                    Log.d(TAG, "缓存数据初始化UI: ${cachedRecords.size} 条记录")
                    
                    val diaryItems = cachedRecords.toDiaryItems()
                    currentState.copy(
                        diaryItems = diaryItems,
                        hasMoreData = true // 初始状态假设有更多数据，由实际分页请求确定
                    )
                } else {
                    currentState
                }
            }
            ?.distinctUntilChanged() // 添加去重，避免相同状态重复触发
            ?.onEach { newState ->
                if (newState != _uiState.value) { // 再次确认状态确实改变了
                    _uiState.value = newState
                }
            }
            ?.launchIn(viewModelScope)
    }

    /**
     * 加载日记列表dw
     * @param isRefresh 是否为刷新操作
     */
    fun loadDiaryList(isRefresh: Boolean = false) {
        if (isLoadingMore && !isRefresh) return

        viewModelScope.launch {
            try {
                if (isRefresh) {
                    currentPage = 1
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = true,
                        errorMessage = null
                    )
                } else {
                    isLoadingMore = true
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        errorMessage = null
                    )
                }

                Log.d(TAG, "加载日记列表: 页码=$currentPage, 刷新=$isRefresh")
                val result = diaryRepository.getDiaryList(currentPage, PAGE_SIZE)

                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        totalPages = data.totalPages
                        val newItems = data.records.toDiaryItems()

                        _uiState.value = if (isRefresh) {
                            _uiState.value.copy(
                                isRefreshing = false,
                                isLoading = false,
                                diaryItems = newItems,
                                hasMoreData = data.hasNext,
                                errorMessage = null
                            )
                        } else {
                            val currentItems = _uiState.value.diaryItems
                            _uiState.value.copy(
                                isLoading = false,
                                diaryItems = currentItems + newItems,
                                hasMoreData = data.hasNext,
                                errorMessage = null
                            )
                        }

                        if (data.hasNext) {
                            currentPage++
                        }

                        Log.d(TAG, "成功加载: ${newItems.size} 条记录")
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "加载失败"
                    Log.e(TAG, "加载失败: $errorMsg")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = errorMsg
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载异常", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "未知错误"
                )
            } finally {
                isLoadingMore = false
            }
        }
    }

    /**
     * 加载更多数据
     */
    fun loadMoreData() {
        if (!_uiState.value.hasMoreData || isLoadingMore) return
        loadDiaryList(false)
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        loadDiaryList(true)
    }

    /**
     * 强制刷新缓存
     */
    fun forceRefreshCache() {
        viewModelScope.launch {
            cacheRepository?.let { repo ->
                Log.d(TAG, "强制刷新缓存")
                _uiState.value = _uiState.value.copy(isRefreshing = true)
                
                val result = repo.refreshCache(PAGE_SIZE * 2) // 刷新前2页数据
                
                if (result.isSuccess) {
                    Log.d(TAG, "缓存刷新成功")
                    // 缓存更新后，监听器会自动更新UI
                } else {
                    Log.e(TAG, "缓存刷新失败")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = "缓存刷新失败"
                    )
                }
            }
        }
    }

    /**
     * 删除日记
     */
    fun deleteDiary(diaryItem: DiaryItem) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "删除日记: ${diaryItem.diaryId}")
                
                val result = diaryRepository.deleteDiary(
                    diaryItem.diaryId,
                    diaryItem.createTimeFormatted
                )

                if (result.isSuccess) {
                    Log.d(TAG, "删除成功")
                    // 从UI中移除已删除的项目
                    val currentItems = _uiState.value.diaryItems
                    val updatedItems = currentItems.filter { it.diaryId != diaryItem.diaryId }
                    
                    _uiState.value = _uiState.value.copy(
                        diaryItems = updatedItems,
                        errorMessage = null
                    )
                } else {
                    Log.e(TAG, "删除失败")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "删除失败"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除异常", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "删除失败"
                )
            }
        }
    }

    /**
     * 获取缓存信息
     */
    fun getCacheInfo() {
        viewModelScope.launch {
            cacheRepository?.let { repo ->
                val cacheInfo = repo.getCacheInfo()
                Log.d(TAG, "缓存信息: 有数据=${cacheInfo.hasData}, 有效=${cacheInfo.isValid}, 更新时间=${cacheInfo.lastUpdateTime}")
            }
        }
    }

    /**
     * 加载用户信息
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val result = userInfoRepository.getUserInfo()
                if (result.isSuccess) {
                    val userInfo = result.getOrNull()
                    userInfo?.let {
                        _uiState.value = _uiState.value.copy(userInfo = it)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载用户信息失败", e)
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}