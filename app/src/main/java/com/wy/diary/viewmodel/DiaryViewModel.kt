package com.wy.diary.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.data.model.DiaryRequest
import com.wy.diary.data.model.DiaryUiState
import com.wy.diary.data.repository.DiaryRepositoryHttpImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


/**
 * 日记视图模型 - 处理UI状态和业务逻辑
 */
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepositoryHttpImpl
) : ViewModel() {
    // 统一的UI状态流
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState
    
    // 为保持兼容性，定义状态访问器
    val editorContent: StateFlow<String> = uiState
        .map { it.editorContent }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.editorContent)
        
    val photos: StateFlow<List<Uri>> = uiState
        .map { it.photos }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.photos)
        
    val address: StateFlow<String> = uiState
        .map { it.address }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.address)
        
    val isSaving: StateFlow<Boolean> = uiState
        .map { it.isSaving }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.isSaving)
        
    val saveError: StateFlow<String?> = uiState
        .map { it.error }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.error)
    
    // 更新方法
    fun updateEditorContent(content: String) {
        _uiState.update { it.copy(editorContent = content) }
    }
    
    fun addPhoto(uri: Uri) {
        _uiState.update { it.copy(photos = it.photos + uri) }
    }
    
    fun removePhoto(uri: Uri) {
        _uiState.update { it.copy(photos = it.photos.filter { photo -> photo != uri }) }
    }
    
    fun updateAddress(newAddress: String) {
        _uiState.update { it.copy(address = newAddress) }
    }
    
    fun clearContent() {
        _uiState.update { it.copy(editorContent = "", photos = emptyList(), address = "未选择地址", error = null) }
    }
    
    /**
     * 保存日记 - 核心业务逻辑
     */
    fun saveDiary(context: Context) {
        viewModelScope.launch {
            // 更新状态为保存中
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            try {
                // 1. 获取日记ID
                val diaryIdResult = repository.getDiaryId()
                val diaryId = diaryIdResult.getOrElse {
                    throw Exception("获取日记ID失败: ${it.message}")
                }
                
                // 2. 上传图片
                val imageUrlsResult = repository.uploadImages(context, _uiState.value.photos, diaryId)
                val imageUrls = imageUrlsResult.getOrElse {
                    throw Exception("上传图片失败: ${it.message}")
                }
                
                // 3. 获取日期信息和用户ID
                val dateInfo = repository.getCurrentDateInfo()
                val openId = repository.getUserOpenId()
                
                // 4. 构建请求并保存日记
                val diaryRequest = DiaryRequest(
                    openId = openId,
                    editorContent = _uiState.value.editorContent,
                    createTime = dateInfo.createTime,
                    logTime = dateInfo.logTime,
                    logWeek = dateInfo.logWeek,
                    logLunar = dateInfo.logLunar,
                    address = _uiState.value.address,
                    imageUrls = imageUrls,
                    diaryId = diaryId
                )
                
                val saveResult = repository.saveDiary(diaryRequest)
                saveResult.fold(
                    onSuccess = { savedDiaryId ->
                        // 保存成功，更新状态并清空内容
                        clearContent()
                        _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
                        Log.d("DiaryViewModel", "日记保存成功，ID: $savedDiaryId")
                    },
                    onFailure = { error ->
                        // 保存失败
                        Log.e("DiaryViewModel", "保存日记失败", error)
                        _uiState.update { 
                            it.copy(
                                isSaving = false, 
                                error = "保存失败: ${error.message}",
                                isSaveSuccess = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("DiaryViewModel", "保存日记过程发生异常", e)
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        error = "保存失败: ${e.message}",
                        isSaveSuccess = false
                    ) 
                }
                // 可以添加回滚逻辑，如删除已上传的图片
            }
        }
    }
    
    /**
     * 清除保存成功状态 - 用于在UI显示成功消息后调用
     */
    fun clearSaveSuccessFlag() {
        _uiState.update { it.copy(isSaveSuccess = false) }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}


