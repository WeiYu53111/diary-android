package com.wy.diary.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hutool.core.date.ChineseDate
import coil.compose.AsyncImage
import com.wy.diary.components.CustomTabBar
import com.wy.diary.data.model.DiaryUiState
import com.wy.diary.data.model.SavingStep
import com.wy.diary.ui.theme.DiaryAndroidTheme

import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiaryScreen(
    uiState: DiaryUiState,
    photos: List<Uri>,
    editorContent: String,
    address: String,
    onAddPhotoClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onEditorContentChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var pageIndex by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主界面内容
        Scaffold(bottomBar = {
            CustomTabBar(
                activeIndex = pageIndex, 
                onTabSelected = { newIndex ->
                    pageIndex = newIndex
                },
                onHistoryClick = onNavigateToHistory
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Header()
                MainContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    photos = photos,
                    editorContent = editorContent,
                    address = address,
                    onAddPhotoClick = onAddPhotoClick,
                    onRemovePhoto = onRemovePhoto,
                    onEditorContentChange = onEditorContentChange,
                    onAddressChange = onAddressChange,
                    isSaving = uiState.isSaving,
                    saveError = uiState.error,
                    onSaveClick = onSaveClick
                )
            }
        }
        
        // 保存进度覆盖层
        if (uiState.isSaving) {
            SavingOverlay(
                step = uiState.savingStep,
                progress = uiState.saveProgress
            )
        }
    }
}

@Composable
fun Header() {
    val calendar = Calendar.getInstance()
    
    // 使用Hutool创建农历日期
    val chineseDate = remember { 
        val date = calendar.time
        ChineseDate(date)
    }
    
    val lunarMonth = remember { chineseDate.chineseMonthName }
    val lunarDay = remember { chineseDate.chineseDay }

    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val weekDayFormat = SimpleDateFormat("EEEE", Locale.CHINESE)

    val date = dateFormat.format(calendar.time)
    val weekday = weekDayFormat.format(calendar.time)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date, style = MaterialTheme.typography.bodyLarge, color = Color.White
            )
            Text(
                text = weekday, style = MaterialTheme.typography.bodyLarge, color = Color.White
            )
            Text(
                text = "农历 $lunarMonth$lunarDay",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    photos: List<Uri>,
    editorContent: String,
    address: String,
    onAddPhotoClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onEditorContentChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    isSaving: Boolean,
    saveError: String?,
    onSaveClick: () -> Unit
) {
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderlined by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // 编辑器部分
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            TextField(
                value = editorContent,
                onValueChange = onEditorContentChange,
                modifier = Modifier
                    .fillMaxSize(),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
                ),
                placeholder = {
                    Text("记录今天的心情...")
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 照片区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 显示已添加的图片
                items(photos) { photoUri ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(width = 1.dp, color = Color.Gray, shape = RoundedCornerShape(8.dp))
                    ) {
                        // 使用AsyncImage加载图片
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "选择的照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 删除按钮
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onRemovePhoto(photoUri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "删除",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 添加图片按钮
                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp, color = Color.Gray, shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onAddPhotoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加照片",
                                tint = Color.Gray
                            )
                            Text(
                                text = "添加照片",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 地址选择
        Row(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { /* 打开地址选择器 */ }
            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "位置",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "选择的地址: $address", style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 提交按钮
        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSaving) "保存中..." else "提交保存")
        }
        
        // 如果有错误，显示错误消息
        saveError?.let {
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SavingOverlay(
    step: SavingStep,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) { /* 拦截点击事件 */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(300.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "正在保存日记",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when(step) {
                        SavingStep.PREPARING -> "正在准备数据..."
                        SavingStep.UPLOADING_IMAGES -> "正在上传图片..."
                        SavingStep.SAVING_CONTENT -> "正在保存日记内容..."
                        SavingStep.FINALIZING -> "正在完成保存..."
                        SavingStep.NONE -> "处理中..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 线性进度条
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 显示百分比
                Text(
                    text = "${(progress * 100).toInt()}%",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiaryScreenPreview() {
    DiaryAndroidTheme {
        DiaryScreen(
            uiState = DiaryUiState(),
            photos = emptyList(),
            editorContent = "今天的心情很好...",
            address = "北京市海淀区",
            onAddPhotoClick = {},
            onRemovePhoto = {},
            onEditorContentChange = {},
            onAddressChange = {},
            onSaveClick = {},
            onNavigateToHistory = {}
        )
    }
}