package com.wy.diary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.wy.diary.components.CustomTabBar
import com.wy.diary.components.DiaryCard
import com.wy.diary.components.EmptyDiaryView
import com.wy.diary.components.UserInfoCard
import com.wy.diary.data.model.DiaryHistoryUIState
import com.wy.diary.data.model.DiaryItem

@Composable
fun DiaryHistoryScreen(
    uiState: DiaryHistoryUIState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onDiaryClick: (DiaryItem) -> Unit,
    onDeleteClick: (DiaryItem) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onWriteClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    val scrollState = rememberLazyListState()
    
    // 监听列表滚动到底部，加载更多数据
    LaunchedEffect(scrollState.layoutInfo.visibleItemsInfo, uiState.diaryItems.size) {
        val lastVisibleItem = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()
        val lastItem = uiState.diaryItems.size - 1
        
        if (lastVisibleItem != null && lastVisibleItem.index == lastItem && uiState.hasMoreData && !uiState.isLoading) {
            // 到达底部，且有更多数据可加载
            onLoadMore()
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
                        onWriteClick()
                    }
                },
                onWriteClick = onWriteClick
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isRefreshing),
            onRefresh = onRefresh,
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
                    onBackupClick = onBackupClick
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
                        onCreateClick = onWriteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)  // 正确的 weight 用法
                    )
                } else {
                    // 日记列表
                    DiaryListContent(
                        diaryItems = uiState.diaryItems,
                        hasMoreData = uiState.hasMoreData,
                        isLoading = uiState.isLoading,
                        scrollState = scrollState,
                        onDiaryClick = onDiaryClick,
                        onDeleteClick = { diary -> diaryToDelete = diary },
                        onImageClick = onImageClick,
                        onLoadMoreClick = onLoadMore
                    )
                }
            }
        }
        
        // 删除确认对话框
        diaryToDelete?.let { diary ->
            DeleteConfirmationDialog(
                onDeleteConfirm = {
                    onDeleteClick(diary)
                    diaryToDelete = null
                },
                onDismiss = { diaryToDelete = null }
            )
        }
    }
}

@Composable
private fun DiaryListContent(
    diaryItems: List<DiaryItem>,
    hasMoreData: Boolean,
    isLoading: Boolean,
    scrollState: LazyListState,
    onDiaryClick: (DiaryItem) -> Unit,
    onDeleteClick: (DiaryItem) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onLoadMoreClick: () -> Unit
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items(diaryItems) { diary ->
            DiaryCard(
                diary = diary,
                onDiaryClick = { onDiaryClick(diary) },
                onDeleteClick = { onDeleteClick(diary) },
                onImageClick = onImageClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        
        // 底部加载状态
        if (hasMoreData || isLoading) {
            item {
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    LoadMoreButton(onLoadMoreClick)
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadMoreButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("加载更多")
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDeleteConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除日记") },
        text = { Text("确定要删除这篇日记吗？") },
        confirmButton = {
            TextButton(onClick = onDeleteConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 预览函数...