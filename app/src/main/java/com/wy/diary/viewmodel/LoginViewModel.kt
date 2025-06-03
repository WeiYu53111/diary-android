package com.wy.diary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.BuildConfig
import com.wy.diary.data.model.LoginUiState
import com.wy.diary.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(private val appRepository: AppRepository) : ViewModel() {
    // UI 状态
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    init {
        // 初始化时检查登录状态
        checkLoginStatus()
    }
    
    /**
     * 检查用户是否已登录并更新UI状态
     */
    fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val isLoggedIn = appRepository.getLoginStatus()
                _uiState.value = _uiState.value.copy(
                    isLoginSuccess = isLoggedIn
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "获取登录状态失败: ${e.message ?: "未知错误"}"
                )
            }
        }
    }
    
    /**
     * 执行微信登录
     */
    fun performWeChatLogin() {
        // 防止重复点击
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 这里实际项目中需要接入微信SDK进行登录
                // 模拟网络请求延迟
                delay(1000)
                
                // 模拟微信登录返回的 open_id
                val mockOpenId = BuildConfig.DEV_OPEN_ID
                
                // 使用 appRepository 保存令牌
                appRepository.saveToken(mockOpenId)
                
                // 同时更新登录状态
                appRepository.saveLoginStatus(true)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "登录失败"
                )
            }
        }
    }
    

    /**
    * 执行登出操作
    */
    fun logout() {
        viewModelScope.launch {
            try {
                // 清除令牌
                appRepository.saveToken("")  // 清除令牌
                
                // 更新登录状态
                appRepository.saveLoginStatus(false)
                
                // 更新 UI 状态
                _uiState.value = _uiState.value.copy(
                    isLoginSuccess = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "登出失败: ${e.message ?: "未知错误"}"
                )
            }
        }
    }


    /**
     * 打开用户协议
     */
    fun openTermsOfService() {
        // 实现打开用户协议的逻辑
        // 可以通过导航或Intent打开相应页面
    }
    
    /**
     * 打开隐私政策
     */
    fun openPrivacyPolicy() {
        // 实现打开隐私政策的逻辑
        // 可以通过导航或Intent打开相应页面
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

