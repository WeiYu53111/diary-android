package com.wy.diary.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.wy.diary.R
import com.wy.diary.components.CustomTabBar
import com.wy.diary.components.DiaryCard
import com.wy.diary.components.EmptyDiaryView
import com.wy.diary.components.UserInfoCard
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.DiaryHistoryViewModel
import com.wy.diary.viewmodel.DiaryItem
import kotlinx.coroutines.launch

class DiaryHistory : ComponentActivity() {
    // 使用 ViewModel
    private val viewModel: DiaryHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            DiaryAndroidTheme {
                DiaryHistoryScreen(viewModel = viewModel)
            }
        }
        
        // 观察错误消息并显示 Toast
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.errorMessage?.let { message ->
                    Toast.makeText(this@DiaryHistory, message, Toast.LENGTH_SHORT).show()
                    viewModel.clearError() // 清除错误消息
                }
            }
        }
    }
    
    // 跳转到创建日记页面
    private fun navigateToCreateDiary() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    // 重写 finish() 方法以添加返回动画
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

@Composable
fun DiaryHistoryScreen(viewModel: DiaryHistoryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    
    // 监听列表滚动到底部，加载更多数据
    LaunchedEffect(scrollState.layoutInfo.visibleItemsInfo, uiState.diaryItems.size) {
        val lastVisibleItem = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()
        val lastItem = uiState.diaryItems.size - 1
        
        if (lastVisibleItem != null && lastVisibleItem.index == lastItem && uiState.hasMoreData && !uiState.isLoading) {
            // 到达底部，且有更多数据可加载
            viewModel.loadDiaryList(false)
        }
    }
    
    // 设置默认选中"我的日记"选项卡（索引 1）
    var activeTabIndex by remember { mutableStateOf(1) }
    
    // 显示删除确认对话框的日记项
    var diaryToDelete by remember { mutableStateOf<DiaryItem?>(null) }
    
    Scaffold(
        bottomBar = {
            CustomTabBar(
                activeIndex = activeTabIndex,
                onTabSelected = { newIndex -> 
                    activeTabIndex = newIndex
                    
                    // 根据选项卡切换页面
                    if (newIndex == 0) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }
                },
                onWriteClick = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isRefreshing),
            onRefresh = { viewModel.loadDiaryList(true) },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 用户信息卡片
                UserInfoCard(
                    diaryCount = uiState.userInfo.diaryCount,
                    registerTime = uiState.userInfo.registerTime,
                    modifier = Modifier.padding(vertical = 16.dp),
                    onBackupClick = {
                        // 跳转到数据备份页面
                        val intent = Intent(context, DataBackupActivity::class.java)
                        context.startActivity(intent)
                    }

                )
                
                // 日记列表标题
                Text(
                    text = "我的日记",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                if (uiState.diaryItems.isEmpty() && !uiState.isLoading) {
                    // 空状态视图
                    EmptyDiaryView(
                        onCreateClick = {
                            context.startActivity(Intent(context, MainActivity::class.java))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    // 日记列表
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(uiState.diaryItems) { diary ->
                            DiaryCard(
                                diary = diary,
                                onDiaryClick = { 
                                    // 跳转到日记详情页，传递完整日记数据
                                    val intent = Intent(context, DiaryDetailActivity::class.java).apply {
                                        // 传递日记ID作为主键
                                        putExtra("DIARY_ID", diary.diaryId)
                                        
                                        // 传递日记基本信息
                                        putExtra("LOG_TIME", diary.logTime)
                                        putExtra("LOG_WEEK", diary.logWeek) 
                                        putExtra("LOG_LUNAR", diary.logLunar)
                                        putExtra("CONTENT", diary.content)  // 内容预览
                                        putExtra("ADDRESS", diary.address)
                                        putExtra("CREATE_TIME", diary.createTimeFormatted)
                                        
                                        // 传递图片列表
                                        putStringArrayListExtra("IMAGES", ArrayList(diary.images))
                                    }
                                    context.startActivity(intent)
                                },
                                onDeleteClick = { 
                                    // 显示删除确认对话框
                                    diaryToDelete = diary
                                },
                                onImageClick = { imageUrls, clickedIndex ->
                                    // 创建Intent跳转到图片预览Activity
                                    val intent = Intent(context, ImagePreviewActivity::class.java).apply {
                                        // 传递所有图片的URL列表和当前点击的图片索引
                                        putStringArrayListExtra("IMAGE_URLS", ArrayList(imageUrls))
                                        putExtra("CURRENT_POSITION", clickedIndex)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                        
                        // 底部加载状态
                        if (uiState.hasMoreData || uiState.isLoading) {
                            item {
                                if (uiState.isLoading) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else {
                                    TextButton(
                                        onClick = { viewModel.loadDiaryList(false) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("加载更多")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 删除确认对话框
        diaryToDelete?.let { diary ->
            AlertDialog(
                onDismissRequest = { diaryToDelete = null },
                title = { Text("删除日记") },
                text = { Text("确定要删除这篇日记吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteDiary(diary)
                            diaryToDelete = null
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { diaryToDelete = null }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// 创建模拟 ViewModel 用于预览
class PreviewDiaryHistoryViewModel : DiaryHistoryViewModel() {
    init {
        // 提供预览数据
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            diaryItems = listOf(
                DiaryItem(
                    diaryId = "1",
                    logTime = "2025年5月15日",
                    logWeek = "星期四",
                    logLunar = "四月初八",
                    contentPreview = "今天天气不错，和朋友一起去公园散步。看到许多人在户外活动，感觉春天真的来了。",
                    content = "今天天气不错，和朋友一起去公园散步。看到许多人在户外活动，感觉春天真的来了。",
                    images = listOf("image1.jpg", "image2.jpg"),
                    createTimeFormatted = "2025-05-15 10:30",
                    address = "北京市海淀区"
                ),
                DiaryItem(
                    diaryId = "2",
                    logTime = "2025年5月14日",
                    logWeek = "星期三",
                    logLunar = "四月初七",
                    contentPreview = "工作很忙，但很充实。完成了一个重要项目，团队都很开心。晚上加班到很晚，明天要早点休息。",
                    content =  "工作很忙，但很充实。完成了一个重要项目，团队都很开心。晚上加班到很晚，明天要早点休息。",
                    images = emptyList(),
                    createTimeFormatted = "2025-05-14 22:15",
                    address = "上海市浦东新区"
                ),
                DiaryItem(
                    diaryId = "3",
                    logTime = "2025年5月13日",
                    logWeek = "星期二",
                    logLunar = "四月初六",
                    contentPreview = "今天是个值得纪念的日子，收到了期待已久的好消息。希望接下来的日子一切顺利。",
                    content = "今天是个值得纪念的日子，收到了期待已久的好消息。希望接下来的日子一切顺利。",
                    images = listOf("image3.jpg"),
                    createTimeFormatted = "2025-05-13 18:45",
                    address = "广州市天河区"
                )
            ),
            userInfo = com.wy.diary.viewmodel.UserInfo(
                diaryCount = 36,
                registerTime = "注册于 2024-11-15"
            ),
            hasMoreData = true,
            isRefreshing = false,
            errorMessage = null
        )
    }
}

// 创建空状态的模拟 ViewModel
class EmptyDiaryHistoryViewModel : DiaryHistoryViewModel() {
    init {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            diaryItems = emptyList(),
            userInfo = com.wy.diary.viewmodel.UserInfo(
                diaryCount = 0,
                registerTime = "注册于 2025-01-01"
            ),
            hasMoreData = false,
            isRefreshing = false,
            errorMessage = null
        )
    }
}

// 创建加载中状态的模拟 ViewModel
class LoadingDiaryHistoryViewModel : DiaryHistoryViewModel() {
    init {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            diaryItems = emptyList(),
            userInfo = com.wy.diary.viewmodel.UserInfo(
                diaryCount = 0,
                registerTime = "注册于 2025-01-01"
            ),
            hasMoreData = false,
            isRefreshing = false,
            errorMessage = null
        )
    }
}

/**
 * 预览正常数据状态
 */
@Preview(name = "日记历史页面 - 有数据", showSystemUi = true)
@Composable
private fun DiaryHistoryScreenWithDataPreview() {
    val previewViewModel = PreviewDiaryHistoryViewModel()
    
    DiaryAndroidTheme {
        DiaryHistoryScreen(viewModel = previewViewModel)
    }
}

/**
 * 预览空数据状态
 */
@Preview(name = "日记历史页面 - 空状态", showSystemUi = true)
@Composable
private fun DiaryHistoryScreenEmptyPreview() {
    val emptyViewModel = EmptyDiaryHistoryViewModel()
    
    DiaryAndroidTheme {
        DiaryHistoryScreen(viewModel = emptyViewModel)
    }
}

/**
 * 预览加载状态
 */
@Preview(name = "日记历史页面 - 加载中", showSystemUi = true)
@Composable
private fun DiaryHistoryScreenLoadingPreview() {
    val loadingViewModel = LoadingDiaryHistoryViewModel()
    
    DiaryAndroidTheme {
        DiaryHistoryScreen(viewModel = loadingViewModel)
    }
}

/**
 * 预览删除确认对话框
 */
@Preview(name = "删除确认对话框", showBackground = true)
@Composable
private fun DeleteConfirmationDialogPreview() {
    DiaryAndroidTheme {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("删除日记") },
            text = { Text("确定要删除这篇日记吗？") },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 预览底部加载中状态
 */
@Preview(name = "底部加载状态", showBackground = true)
@Composable
private fun LoadingMorePreview() {
    DiaryAndroidTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * 预览加载更多按钮
 */
@Preview(name = "加载更多按钮", showBackground = true)
@Composable
private fun LoadMoreButtonPreview() {
    DiaryAndroidTheme {
        TextButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("加载更多")
        }
    }
}
