package com.wy.diary.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wy.diary.R
import com.wy.diary.data.model.DiaryDetailItem
import com.wy.diary.data.model.DiaryDetailUiState
import com.wy.diary.ui.theme.DiaryAndroidTheme

/**
 * 日记详情屏幕
 * 该函数接收UI状态和回调函数，而不是直接依赖ViewModel
 */
@Composable
fun DiaryDetailScreen(
    uiState: DiaryDetailUiState,
    onBackClick: () -> Unit,
    onEditClick: (DiaryDetailItem) -> Unit,
    onImageClick: (List<String>, Int) -> Unit
) {
    // 获取系统栏的高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    Scaffold(
        topBar = {
            // 添加状态栏高度的 padding
            Box(modifier = Modifier.padding(top = statusBarHeight)) {
                CustomTopBar(
                    title = "日记详情",
                    onBackClick = onBackClick
                )
            }
        },
        floatingActionButton = {
            // 编辑按钮
            uiState.diary?.let { diary ->
                FloatingActionButton(
                    onClick = { onEditClick(diary) }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑日记")
                }
            }
        },
        // 移除系统默认 padding，我们会在内容中自己处理
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        DiaryDetailContent(
            uiState = uiState,
            onImageClick = onImageClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun DiaryDetailContent(
    uiState: DiaryDetailUiState,
    onImageClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            uiState.isLoading -> {
                // 加载状态
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.diary != null -> {
                // 正常内容状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 日期信息卡片
                    DateCard(
                        date = uiState.diary.logTime,
                        week = uiState.diary.logWeek,
                        lunar = uiState.diary.logLunar
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 日记内容
                    DiaryContent(content = uiState.diary.content)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 图片网格（如果有图片）
                    if (uiState.diary.images.isNotEmpty()) {
                        ImagesGrid(
                            images = uiState.diary.images,
                            onImageClick = { clickedIndex ->
                                onImageClick(uiState.diary.images, clickedIndex)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 底部信息：位置和创建时间
                    BottomInfoBar(
                        address = uiState.diary.address,
                        createTime = uiState.diary.createTimeFormatted
                    )
                }
            }
            else -> {
                // 错误或空状态
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("无法加载日记详情")
                }
            }
        }
    }
}

@Composable
private fun DiaryContent(content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        // 设置卡片背景色与父容器一致
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent, // 透明背景，继承父容器颜色
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val paragraphs = content.split("\n\n")
            Column {
                paragraphs.forEachIndexed { index, paragraph ->
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        softWrap = true
                    )

                    // 段落之间添加间距，最后一段除外
                    if (index < paragraphs.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagesGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    Text(
        text = "附图",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 使用固定的 Box 网格，而不是 LazyVerticalGrid
    val imageCount = images.size
    val rows = (imageCount + 2) / 3 // 向上取整行数
    
    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    if (index < imageCount) {
                        val imageUrl = images[index]

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "日记图片",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(index) },
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.image_error),
                            error = painterResource(id = R.drawable.image_error)
                        )
                    } else {
                        // 补充空白位置，保持网格对齐
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomInfoBar(
    address: String,
    createTime: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = "位置",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = address,
            style = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "创建于 $createTime",
            style = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun DateCard(date: String, week: String, lunar: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日期圆圈
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = date.substringAfterLast("月").removeSuffix("日"),
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = date,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$week · $lunar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CustomTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .height(48.dp)  // 固定高度确保足够的点击区域
        ) {
            // 返回按钮 - 增大点击区域
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBackClick)
                    .padding(12.dp)  // 内部padding，图标实际大小
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 居中标题 - 确保不会被按钮遮挡
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)  // 左右留出按钮的空间
            )
        }
    }
}

// 更新预览函数
@Preview(showBackground = true)
@Composable
fun DiaryDetailPreview() {
    // 创建预览数据，而不是实例化ViewModel
    val previewState = DiaryDetailUiState(
        isLoading = false,
        diary = DiaryDetailItem(
            diaryId = "1",
            logTime = "2025年5月15日",
            logWeek = "星期四",
            logLunar = "四月初八",
            content = "今天天气不错，和朋友一起去公园散步。看到许多人在户外活动，感觉春天真的来了。\n\n" +
                "下午去了图书馆，借了几本很久想看的书。在那里安静地度过了几个小时，感觉很充实。\n\n" +
                "晚上和家人一起吃了饭，聊了很多最近发生的事情。总的来说，这是很平静但很满足的一天。",
            images = listOf(
                "https://www.baidu.com/img/PCfb_5bf082d29588c07f842ccde3f97243ea.png",
                "https://www.baidu.com/img/PCfb_5bf082d29588c07f842ccde3f97243ea.png"
            ),
            createTimeFormatted = "2025-05-15 10:30",
            address = "北京市海淀区"
        ),
        errorMessage = null
    )
    
    DiaryAndroidTheme {
        DiaryDetailScreen(
            uiState = previewState,
            onBackClick = {},
            onEditClick = {},
            onImageClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "加载状态")
@Composable
fun DiaryDetailLoadingPreview() {
    val loadingState = DiaryDetailUiState(isLoading = true)
    
    DiaryAndroidTheme {
        DiaryDetailScreen(
            uiState = loadingState,
            onBackClick = {},
            onEditClick = {},
            onImageClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "错误状态")
@Composable
fun DiaryDetailErrorPreview() {
    val errorState = DiaryDetailUiState(
        isLoading = false,
        diary = null,
        errorMessage = "无法加载日记"
    )
    
    DiaryAndroidTheme {
        DiaryDetailScreen(
            uiState = errorState,
            onBackClick = {},
            onEditClick = {},
            onImageClick = { _, _ -> }
        )
    }
}