package com.wy.diary.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.wy.diary.data.model.DiaryItem
import com.wy.diary.ui.screen.DiaryDetailScreen
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.DiaryDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiaryDetailActivity : ComponentActivity() {
    
    // 使用依赖注入获取 ViewModel
    private val viewModel: DiaryDetailViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 配置边缘到边缘显示
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        
        // 从 Intent 中获取日记 ID，并加载数据
        val diary = intent.getParcelableExtra<DiaryItem>("DIARY_RECORD")!!
        viewModel.loadDiary(diary)
        
        // 设置界面内容
        setContent {
            // 收集 ViewModel 的状态
            val uiState by viewModel.uiState.collectAsState()
            
            DiaryAndroidTheme {
                // 直接使用 DiaryDetailScreen 并传递状态和回调
                DiaryDetailScreen(
                    uiState = uiState,
                    onBackClick = { finish() },
                    onEditClick = { diary ->
                        // 跳转到编辑页面
//                        startActivity(
//                            intent = android.content.Intent(this, WriteActivity::class.java).apply {
//                                putExtra("EDIT_DIARY_ID", diary.diaryId)
//                            }
//                        )
                    },
                    onImageClick = { imageUrls, clickedIndex ->
                        // 跳转到图片预览页面
                        startActivity(
                            android.content.Intent(this, ImagePreviewActivity::class.java).apply {
                                putStringArrayListExtra("IMAGE_URLS", ArrayList(imageUrls))
                                putExtra("CURRENT_POSITION", clickedIndex)
                            }
                        )
                    }
                )
            }
        }
        
        // 观察错误消息并显示 Toast
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.errorMessage?.let { message ->
                    Toast.makeText(this@DiaryDetailActivity, message, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }
    
    // 添加自定义返回动画
    override fun finish() {
        super.finish()
        overridePendingTransition(com.wy.diary.R.anim.slide_in_left, com.wy.diary.R.anim.slide_out_right)
    }
}