package com.wy.diary.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.wy.diary.R
import com.wy.diary.data.model.DiaryItem
import com.wy.diary.data.model.DiaryRecord
import com.wy.diary.ui.screen.DiaryHistoryScreen
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.DiaryHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiaryHistoryActivity : ComponentActivity() {
    // 使用 ViewModel
    private val viewModel: DiaryHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaryAndroidTheme {
                val uiState by viewModel.uiState.collectAsState()
                
                DiaryHistoryScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadDiaryList(true) },
                    onLoadMore = { viewModel.loadDiaryList(false) },
                    onDiaryClick = { diary -> navigateToDiaryDetail(diary) },
                    onDeleteClick = { diary -> viewModel.deleteDiary(diary) },
                    onImageClick = { imageUrls, clickedIndex -> navigateToImagePreview(imageUrls, clickedIndex) },
                    onWriteClick = { navigateToWriteDiary() },
                    onBackupClick = { navigateToDataBackup() }
                )
            }
        }
        
        // 观察错误消息并显示 Toast
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.errorMessage?.let { message ->
                    Toast.makeText(this@DiaryHistoryActivity, message, Toast.LENGTH_SHORT).show()
                    viewModel.clearError() // 清除错误消息
                }
            }
        }
    }
    
    private fun navigateToDiaryDetail(diary:DiaryItem) {
        val intent = Intent(this, DiaryDetailActivity::class.java).apply {
            putExtra("DIARY_RECORD", diary)
        }
        startActivity(intent)
    }
    
    private fun navigateToImagePreview(imageUrls: List<String>, clickedIndex: Int) {
        val intent = Intent(this, ImagePreviewActivity::class.java).apply {
            putStringArrayListExtra("IMAGE_URLS", ArrayList(imageUrls))
            putExtra("CURRENT_POSITION", clickedIndex)
        }
        startActivity(intent)
    }
    
    private fun navigateToWriteDiary() {
        val intent = Intent(this, WriteActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToDataBackup() {
        val intent = Intent(this, DataBackupActivity::class.java)
        startActivity(intent)
    }

    // 重写 finish() 方法以添加返回动画
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
