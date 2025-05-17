package com.wy.diary.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.wy.diary.R
import com.wy.diary.model.BackupTaskStatus
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.ActionEvent
import com.wy.diary.viewmodel.BackupFileInfo
import com.wy.diary.viewmodel.BackupState
import com.wy.diary.viewmodel.DataBackupViewModel
import com.wy.diary.viewmodel.IDataBackupViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DataBackupActivity : ComponentActivity() {
    
    // 使用自定义Factory创建ViewModel
    private val viewModel: DataBackupViewModel by viewModels { 
        DataBackupViewModel.Factory(application) 
    }
    
    // 添加文件创建启动器
    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>
    private var pendingTaskId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 注册文件创建启动器
        createDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            uri?.let {
                pendingTaskId?.let { taskId ->
                    viewModel.saveBackupFile(taskId, uri)
                    pendingTaskId = null
                }
            }
        }
        
        // 收集 actionEvent 流以处理事件
        lifecycleScope.launch {
            viewModel.actionEvent.collect { event ->
                handleActionEvent(event)
            }
        }
        
        setContent {
            DiaryAndroidTheme {
                DataBackupScreen(
                    onNavigateBack = { finish() },
                    viewModel = viewModel
                )
            }
        }
    }
    
    // 处理 ActionEvent 事件
    private fun handleActionEvent(event: ActionEvent?) {
        when (event) {
            is ActionEvent.RequestSaveLocation -> {
                Log.d("DataBackupActivity", "收到保存位置请求事件: ${event.fileName}")
                
                // 保存 taskId 以备后用
                pendingTaskId = event.taskId
                
                // 启动文件选择器
                createDocumentLauncher.launch(event.fileName)
                
                // 清除已处理的事件
                viewModel.clearActionEvent()
            }
            is ActionEvent.ShowDialog -> {
                // 显示提示对话框
                AlertDialog.Builder(this)
                    .setTitle(event.title)
                    .setMessage(event.message)
                    .setPositiveButton("确定") { _, _ -> event.confirmAction() }
                    .setNegativeButton("取消") { _, _ -> event.cancelAction() }
                    .show()
                
                // 清除已处理的事件
                viewModel.clearActionEvent()
            }
            null -> { /* 忽略 null 事件 */ }
        }
    }
    
    // 重写 finish() 方法以添加返回动画
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: IDataBackupViewModel
) {
    val backupState by viewModel.backupState.collectAsState()
    val backupFiles by viewModel.backupFiles.collectAsState() // 收集文件状态
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = Unit) {
        viewModel.checkBackupStatus()
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
            backupState = backupState,
            downloadedFiles = backupFiles, // 传递文件列表
            onBackupClick = {
                scope.launch {
                    viewModel.startBackup()
                    snackbarHostState.showSnackbar("备份已开始")
                }
            },
            onDownloadClick = { taskId ->
                scope.launch {
                    viewModel.downloadBackupFile(taskId)
                    snackbarHostState.showSnackbar("开始下载备份文件")
                }
            },
            onRetryClick = { viewModel.checkBackupStatus() },
            onDeleteFileClick = { fileId ->
                scope.launch {
                    viewModel.deleteBackupFile(fileId)
                    snackbarHostState.showSnackbar("备份文件已删除")
                }
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun DataBackupContent(
    backupState: BackupState,
    downloadedFiles: List<BackupFileInfo>, // 添加文件列表参数
    onBackupClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onDeleteFileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 备份状态显示部分
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            when (backupState) {
                is BackupState.Loading -> {
                    CircularProgressIndicator()
                }
                is BackupState.HasRunningTask -> {
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
                is BackupState.Ready -> {
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
                is BackupState.Error -> {
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetryClick) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        // 使用传入的下载文件列表而不是从状态中获取
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

// 预览部分可以保留，用于设计时预览界面

// 预览加载状态
@Preview(name = "加载中", showBackground = true)
@Composable
fun DataBackupLoadingPreview() {
    DiaryAndroidTheme {
        DataBackupContent(
            backupState = BackupState.Loading,
            downloadedFiles = emptyList(),
            onBackupClick = {},
            onDownloadClick = {},
            onRetryClick = {},
            onDeleteFileClick = {}
        )
    }
}

// 预览正在执行备份状态
@Preview(name = "备份中", showBackground = true)
@Composable
fun DataBackupRunningPreview() {
    DiaryAndroidTheme {
        DataBackupContent(
            backupState = BackupState.HasRunningTask,
            downloadedFiles = emptyList(),
            onBackupClick = {},
            onDownloadClick = {},
            onRetryClick = {},
            onDeleteFileClick = {}
        )
    }
}

// 预览准备备份状态（有上次备份时间）
@Preview(name = "准备备份(有记录)", showBackground = true)
@Composable
fun DataBackupReadyWithTimePreview() {
    DiaryAndroidTheme {
        DataBackupContent(
            backupState = BackupState.Ready("2025-05-15 14:30:25"),
            downloadedFiles = emptyList(),
            onBackupClick = {},
            onDownloadClick = {},
            onRetryClick = {},
            onDeleteFileClick = {}
        )
    }
}

// 预览错误状态
@Preview(name = "错误状态", showBackground = true)
@Composable
fun DataBackupErrorPreview() {
    DiaryAndroidTheme {
        DataBackupContent(
            backupState = BackupState.Error("网络连接失败"),
            downloadedFiles = emptyList(),
            onBackupClick = {},
            onDownloadClick = {},
            onRetryClick = {},
            onDeleteFileClick = {}
        )
    }
}

// 预览完整屏幕
@Preview(name = "完整屏幕", showBackground = true)
@Composable
fun DataBackupScreenPreview() {
    // 预览用的ViewModel实现
    class PreviewDataBackupViewModel : IDataBackupViewModel {
        override val backupState = MutableStateFlow<BackupState>(
            BackupState.Ready(taskId = "123", hasBackupFile = true, taskStatus = BackupTaskStatus.COMPLETED.name)
        )
        override val backupFiles = MutableStateFlow<List<BackupFileInfo>>(emptyList())
        override fun checkBackupStatus() {}
        override fun startBackup() {}
        override fun downloadBackupFile(taskId: String) {}
        override fun deleteBackupFile(fileId: String) {} // 添加删除方法实现
    }
    
    DiaryAndroidTheme {
        DataBackupScreen(
            onNavigateBack = {},
            viewModel = PreviewDataBackupViewModel()
        )
    }
}

// 添加一个带下载文件的预览
@Preview(name = "有下载文件", showBackground = true)
@Composable
fun DataBackupWithFilesPreview() {
    val files = listOf(
        BackupFileInfo(
            id = "file1",
            fileName = "backup_2025-05-16.zip",
            fileSize = "2.4 MB",
            downloadDate = "2025-05-16 12:30:45",
            filePath = "/storage/emulated/0/Download/backup_2025-05-16.zip"
        ),
        BackupFileInfo(
            id = "file2",
            fileName = "backup_2025-05-10.zip",
            fileSize = "1.8 MB",
            downloadDate = "2025-05-10 09:15:30",
            filePath = "/storage/emulated/0/Download/backup_2025-05-10.zip"
        )
    )
    
    DiaryAndroidTheme {
        DataBackupContent(
            backupState = BackupState.Ready(
                lastBackupTime = "2025-05-16 12:30:45",
                taskId = "task123",
                hasBackupFile = true,
                taskStatus = BackupTaskStatus.COMPLETED.name
            ),
            downloadedFiles = files,
            onBackupClick = {},
            onDownloadClick = {},
            onRetryClick = {},
            onDeleteFileClick = {}
        )
    }
}