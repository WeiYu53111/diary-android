package com.wy.diary.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import cn.hutool.core.date.ChineseDate
import android.widget.Toast
import com.wy.diary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// 导入新的组件
import com.wy.diary.components.CustomTabBar

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    // 创建一个共享的 ViewModel
    private val diaryViewModel = DiaryViewModel()

    // 定义照片选择启动器
    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        // 如果用户未登录，跳转到登录页面
        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 修改照片选择器的回调处理
        photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                diaryViewModel.addPhoto(it)
            }
        }

        enableEdgeToEdge()
        setContent {
            DiaryAndroidTheme {
                DiaryApp(
                    onAddPhotoClick = { openPhotoPicker() },
                    diaryViewModel = diaryViewModel,
                    onNavigateToHistory = { navigateToHistoryPage() } // 添加历史页面导航回调
                )
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
        val intent = Intent(this, DiaryHistory::class.java)
        startActivity(intent)
        // 应用自定义过渡动画
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }


}

@Composable
fun DiaryApp(
    onAddPhotoClick: () -> Unit = {},
    diaryViewModel: DiaryViewModel = remember { DiaryViewModel() },
    onNavigateToHistory: () -> Unit = {} // 添加导航到历史页面的回调
) {
    var pageIndex by remember { mutableStateOf(0) }
    
    // 使用 ViewModel 的 photos 状态
    val photos by diaryViewModel.photos.collectAsState()

    Scaffold(bottomBar = {
        CustomTabBar(
            activeIndex = pageIndex, 
            onTabSelected = { newIndex ->
                pageIndex = newIndex
            },
            onHistoryClick = onNavigateToHistory // 传递历史页面跳转回调
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
                onAddPhotoClick = onAddPhotoClick,
                onRemovePhoto = { diaryViewModel.removePhoto(it) },
                diaryViewModel = diaryViewModel
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
    photos: List<Uri> = emptyList(),
    onAddPhotoClick: () -> Unit = {},
    onRemovePhoto: (Uri) -> Unit = {},
    diaryViewModel: DiaryViewModel
) {
    val text by diaryViewModel.editorContent.collectAsState()
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderlined by remember { mutableStateOf(false) }
    val address by diaryViewModel.address.collectAsState()

    // 保存状态
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // 获取上下文
    val context = LocalContext.current

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
                value = text,
                onValueChange = { diaryViewModel.updateEditorContent(it) },
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
                    // 使文本框与Surface颜色匹配
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    // 移除边框
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
            onClick = { 
                // 保存日记数据
                isSaving = true
                saveError = null
                
                // 使用ViewModel保存日记
                diaryViewModel.updateEditorContent(text)
                diaryViewModel.updateAddress(address)
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        diaryViewModel.saveDiary(context)
                        
                        withContext(Dispatchers.Main) {
                            isSaving = false
                            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isSaving = false
                            saveError = "保存失败: ${e.message}"
                            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            enabled = !isSaving // 保存过程中禁用按钮
        ) {
            if (isSaving) {
                // 显示加载指示器
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



@Preview(showBackground = true)
@Composable
fun DiaryAppPreview() {
    DiaryAndroidTheme {
        DiaryApp()
    }
}