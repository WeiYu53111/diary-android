package com.wy.diary.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.wy.diary.R
import com.wy.diary.ui.screen.DiaryScreen
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.DiaryViewModel
import com.wy.diary.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WriteActivity : ComponentActivity() {
    // 创建一个共享的 ViewModel
    private val diaryViewModel: DiaryViewModel by viewModels()

    // 创建一个共享的 ViewModel
    private val loginViewModel: LoginViewModel by viewModels()

    // 定义照片选择启动器
    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化界面
        initializeUI()
    }

    private fun initializeUI() {
        // 初始化照片选择器
        photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                diaryViewModel.addPhoto(it)
            }
        }

        enableEdgeToEdge()
        setContent {
            DiaryAndroidTheme {
                val uiState by diaryViewModel.uiState.collectAsState()
                val photos by diaryViewModel.photos.collectAsState()
                val editorContent by diaryViewModel.editorContent.collectAsState()
                val address by diaryViewModel.address.collectAsState()
                
                DiaryScreen(
                    uiState = uiState,
                    photos = photos,
                    editorContent = editorContent,
                    address = address,
                    onAddPhotoClick = { openPhotoPicker() },
                    onRemovePhoto = { diaryViewModel.removePhoto(it) },
                    onEditorContentChange = { diaryViewModel.updateEditorContent(it) },
                    onAddressChange = { diaryViewModel.updateAddress(it) },
                    onSaveClick = { 
                        // 保存日记
                        diaryViewModel.saveDiary(this@WriteActivity)
                    },
                    onNavigateToHistory = { navigateToHistoryPage() }
                )
            }
        }
        
        // 观察保存状态并显示提示
        lifecycleScope.launch {
            diaryViewModel.uiState.collect { state ->
                if (state.isSaveSuccess) {
                    Toast.makeText(this@WriteActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    diaryViewModel.clearSaveSuccessFlag() // 清除保存成功标志
                }
                
                state.error?.let { error ->
                    Toast.makeText(this@WriteActivity, error, Toast.LENGTH_LONG).show()
                    diaryViewModel.clearError() // 清除错误信息
                }
            }
        }
    }

    private fun openPhotoPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
        )
    }

    // 添加导航到历史页面的方法
    private fun navigateToHistoryPage() {
        val intent = Intent(this, DiaryHistoryActivity::class.java)
        startActivity(intent)
        // 应用自定义过渡动画
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // 重写back按钮方法添加动画
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}