package com.wy.diary.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.wy.diary.ui.screen.ImagePreviewScreen
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.ImagePreviewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImagePreviewActivity : ComponentActivity() {

    private val viewModel: ImagePreviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏显示
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 添加返回键处理
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // 关闭当前活动
            }
        })
        
        // 获取传递的参数
        val imageUrls = intent.getStringArrayListExtra("IMAGE_URLS") ?: ArrayList()
        val initialPosition = intent.getIntExtra("CURRENT_POSITION", 0)
        
        // 初始化ViewModel
        viewModel.initialize(imageUrls, initialPosition)
        
        setContent {
            DiaryAndroidTheme {
                ImagePreviewScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}