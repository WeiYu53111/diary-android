package com.wy.diary.ui.screen

import android.app.Application
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wy.diary.data.model.BackupFileInfo
import com.wy.diary.data.model.BackupState
import com.wy.diary.data.model.BackupTaskStatus
import com.wy.diary.ui.theme.DiaryAndroidTheme
import dagger.hilt.android.internal.Contexts.getApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(
    backupState: StateFlow<BackupState>,
    backupFiles: StateFlow<List<BackupFileInfo>>,
    onNavigateBack: () -> Unit,
    checkBackUPStatusFun :() -> Unit,
    startBackupFun: ()-> Unit,
    downloadBackupFileFun: (taskId: String)-> Unit,
    deleteBackupFileFun:(fieldId:String)-> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = Unit) {
        checkBackUPStatusFun()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据备份") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        DataBackupContent(
            backupStateFlow = backupState,
            downloadedFiles = backupFiles,// 传递文件列表
            onBackupClick = {
                scope.launch {
                    startBackupFun()
                    snackbarHostState.showSnackbar("备份已开始")
                }
            },
            onDownloadClick = { taskId ->
                scope.launch {
                    downloadBackupFileFun(taskId)
                    snackbarHostState.showSnackbar("开始下载备份文件")
                }
            },
            onRetryClick = { checkBackUPStatusFun() },
            onDeleteFileClick = { fileId ->
                scope.launch {
                    deleteBackupFileFun(fileId)
                    snackbarHostState.showSnackbar("备份文件已删除")
                }
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun DataBackupContent(
    backupStateFlow: StateFlow<BackupState>,
    downloadedFiles: StateFlow<List<BackupFileInfo>>,
    onBackupClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onDeleteFileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 收集状态流的值
    val backupState by backupStateFlow.collectAsState()
    val filesList by downloadedFiles.collectAsState() 
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部边距
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // 备份状态显示部分
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                // 在状态处理中添加下载状态
                when (backupState) {
                    is BackupState.Loading -> CircularProgressIndicator()
                    is BackupState.HasRunningTask -> RunningBackupContent()
                    is BackupState.Ready -> ReadyBackupContent(
                        backupState = backupState as BackupState.Ready,
                        onBackupClick = onBackupClick,
                        onDownloadClick = onDownloadClick
                    )
                    is BackupState.Error -> ErrorBackupContent(
                        errorMessage = (backupState as BackupState.Error).errorMessage,
                        onRetryClick = onRetryClick
                    )
                    is BackupState.Downloading -> DownloadingContent(progress = (backupState as BackupState.Downloading).progress)
                }
            }
        }

        // 如果有下载文件，添加文件列表标题和分隔线
        if (filesList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "已下载的备份文件",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 文件列表项
            items(filesList) { fileInfo ->
                BackupFileItem(
                    fileInfo = fileInfo,
                    onDeleteClick = { onDeleteFileClick(fileInfo.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // 底部边距
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RunningBackupContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = "备份任务正在进行中...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请稍后再来下载",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyBackupContent(
    backupState: BackupState.Ready,
    onBackupClick: () -> Unit,
    onDownloadClick: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "备份您的数据到本地",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (backupState.taskStatus == BackupTaskStatus.COMPLETED.name) {
            Button(onClick = { onDownloadClick(backupState.taskId) }) {
                Text("下载备份文件")
            }
            Spacer(modifier = Modifier.height(1.dp))
            OutlinedButton(onClick = onBackupClick) {
                Text("重新备份")
            }
        } else {
            Button(onClick = onBackupClick) {
                Text("开始备份")
            }
        }
    }
}

@Composable
private fun ErrorBackupContent(
    errorMessage: String,
    onRetryClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "获取备份状态失败",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text("重试")
        }
    }
}

// 添加下载中的UI组件
@Composable
private fun DownloadingContent(progress: Float) {
    // 添加一个小的呼吸动画效果
    val infiniteTransition = rememberInfiniteTransition(label = "downloading")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(vertical = 16.dp)
            .scale(pulseSize)
    ) {
        // 动态下载信息
        val downloadMessage = when {
            progress < 0.1f -> "准备下载..."
            progress < 0.3f -> "正在连接服务器..."
            progress < 0.9f -> "正在下载备份文件..."
            progress < 1.0f -> "正在完成下载..."
            else -> "下载完成!"
        }
        
        Text(
            text = downloadMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 显示进度条
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(0.7f),
            color = if (progress >= 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 显示百分比，保留一位小数
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (progress >= 1.0f) FontWeight.Bold else FontWeight.Normal
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 根据进度显示不同的提示信息
        val hintMessage = when {
            progress < 0.5f -> "请勿关闭应用..."
            progress < 0.9f -> "正在处理数据..."
            else -> "即将完成..."
        }
        
        Text(
            text = hintMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DownloadedFilesSection(
    downloadedFiles: List<BackupFileInfo>,
    onDeleteFileClick: (String) -> Unit
) {
    if (downloadedFiles.isNotEmpty()) {
        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "已下载的备份文件",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        downloadedFiles.forEach { fileInfo ->
            BackupFileItem(
                fileInfo = fileInfo,
                onDeleteClick = { onDeleteFileClick(fileInfo.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun BackupFileItem(
    fileInfo: BackupFileInfo,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fileInfo.fileName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "大小: ${fileInfo.fileSize}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "下载日期: ${fileInfo.downloadDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除文件",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// 预览部分
@Preview(name = "加载中", showBackground = true)
@Composable
fun DataBackupLoadingPreview() {
    val backupStateFlow = remember { MutableStateFlow<BackupState>(BackupState.Loading) }
    val downloadedFilesFlow = remember { MutableStateFlow<List<BackupFileInfo>>(emptyList()) }
    
    DiaryAndroidTheme {
        DataBackupContent(
            backupStateFlow = backupStateFlow,
            downloadedFiles = downloadedFilesFlow,
            onBackupClick = {},
            onDownloadClick = {},
            onRetryClick = {},
            onDeleteFileClick = {}
        )
    }
}

@Preview(name = "备份中", showBackground = true)
@Composable
fun DataBackupRunningPreview() {
    val backupStateFlow = remember { MutableStateFlow<BackupState>(BackupState.HasRunningTask) }
    val downloadedFilesFlow = remember { MutableStateFlow<List<BackupFileInfo>>(emptyList()) }
    
    DiaryAndroidTheme {
        DataBackupContent(
            backupStateFlow = backupStateFlow,
            downloadedFiles = downloadedFilesFlow,
            onBackupClick = {},
            onDownloadClick = {},
            onRetryClick = {},
            onDeleteFileClick = {}
        )
    }
}

// 其他预览函数...

// 预览完整屏幕
@Preview(name = "完整屏幕", showBackground = true)
@Composable
fun DataBackupScreenPreview() {
    // 创建预览用的状态流
    val backupStateFlow = remember { 
        MutableStateFlow<BackupState>(
            BackupState.Ready(
                taskId = "123",
                hasBackupFile = true,
                taskStatus = BackupTaskStatus.COMPLETED.name
            )
        ) 
    }
    
    val backupFilesFlow = remember { 
        MutableStateFlow(
            listOf(
                BackupFileInfo(
                    id = "1",
                    fileName = "backup_20250601.zip",
                    fileSize = "2.5 MB",
                    downloadDate = "2025-06-01 10:30",
                    filePath = "/storage/emulated/0/Download/backup_20250601.zip"
                ),
                BackupFileInfo(
                    id = "2",
                    fileName = "backup_20250530.zip",
                    fileSize = "1.8 MB",
                    downloadDate = "2025-05-30 15:45",
                    filePath = "/storage/emulated/0/Download/backup_20250530.zip"
                )
            )
        )
    }

    DiaryAndroidTheme {
        DataBackupScreen(
            backupState = backupStateFlow,
            backupFiles = backupFilesFlow,
            onNavigateBack = {},
            checkBackUPStatusFun = {},
            startBackupFun = {},
            downloadBackupFileFun = {},
            deleteBackupFileFun = {}
        )
    }
}