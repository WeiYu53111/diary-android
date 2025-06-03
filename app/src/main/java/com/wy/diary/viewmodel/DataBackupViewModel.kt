package com.wy.diary.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.data.model.ActionEvent
import com.wy.diary.data.model.BackupFileInfo
import com.wy.diary.data.model.BackupState
import com.wy.diary.data.model.BackupTaskStatus
import com.wy.diary.data.remote.BackupApi
import com.wy.diary.data.repository.AppRepository
import com.wy.diary.util.BackupFileManager
import com.wy.diary.util.FileUtils.getFilePathFromUri
import com.wy.diary.util.FileUtils.getFileSizeFromUri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DataBackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupApi: BackupApi,
    private val appRepository: AppRepository
) : ViewModel() {

    private var isDownloading = false
    // UI状态
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Loading)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    // 事件流
    private val _actionEvent = MutableStateFlow<ActionEvent?>(null)
    val actionEvent: StateFlow<ActionEvent?> = _actionEvent.asStateFlow()
    
    // 备份文件列表
    private val _backupFiles = MutableStateFlow<List<BackupFileInfo>>(emptyList())
    val backupFiles: StateFlow<List<BackupFileInfo>> = _backupFiles.asStateFlow()
    
    // 文件管理器
    private val fileManager = BackupFileManager(context)

    // 权限事件流
    private val _permissionEvent = MutableStateFlow<PermissionEvent?>(null)
    val permissionEvent: StateFlow<PermissionEvent?> = _permissionEvent.asStateFlow()

    // 添加定时查询任务控制变量
    private var statusCheckJob: Job? = null

    // 状态检查间隔（毫秒）
    private val STATUS_CHECK_INTERVAL = 5000L // 5秒

    // 最大检查次数，防止无限循环
    private val MAX_CHECK_COUNT = 60 // 最多检查5分钟 (5秒 * 60 = 5分钟)

    init {
        // 初始化时加载已保存的备份文件信息
        loadBackupFiles()
    }
    
    /**
     * 加载已保存的备份文件信息
     */
    private fun loadBackupFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = fileManager.getBackupFiles()
            _backupFiles.value = files
        }
    }

    /**
     * 检查备份状态
     */
    fun checkBackupStatus() {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            try {
                val response = backupApi.getBackupStatus()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.isSuccess()) {
                        val status = apiResponse.data?.status ?: ""
                        val taskId = apiResponse.data?.taskId ?: "-1"
                        val taskStatus = BackupTaskStatus.valueOf(status)

                        if (taskStatus == BackupTaskStatus.COMPLETED) {
                            _backupState.value = BackupState.Ready(
                                taskId = taskId,
                                hasBackupFile = true,
                                taskStatus = BackupTaskStatus.COMPLETED.name
                            )
                        } else if (taskStatus == BackupTaskStatus.PROCESSING) {
                            _backupState.value = BackupState.HasRunningTask
                        } else {
                            _backupState.value = BackupState.Ready(
                                hasBackupFile = false,
                                taskStatus = taskStatus.name
                            )
                        }
                    } else {
                        _backupState.value =
                            BackupState.Error(apiResponse.message ?: "获取备份状态失败")
                    }
                } else {
                    _backupState.value = BackupState.Error("获取备份状态失败：${response.message()}")
                }
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "网络请求异常")
            }
        }
    }

    /**
     * 开始备份
     */
    fun startBackup() {
        viewModelScope.launch {
            try {
                val response = backupApi.startBackup()
                if (response.isSuccessful) {
                    _backupState.value = BackupState.HasRunningTask

                    // 开始备份后，启动定时查询任务
                    startStatusCheckTask()

                } else {
                    _backupState.value = BackupState.Error("启动备份失败：${response.message()}")
                }
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "备份启动失败")
            }
        }
    }

    /**
     * 启动定时查询备份状态的任务
     */
    private fun startStatusCheckTask() {
        // 如果正在下载，不启动状态查询
        if (isDownloading) {
            return
        }
        
        // 取消正在运行的任务（如果有）
        stopStatusCheckTask()
        
        // 创建新的定时任务
        statusCheckJob = viewModelScope.launch {
            var checkCount = 0
            
            while (checkCount < MAX_CHECK_COUNT) {
                // 如果开始下载，停止状态检查
                if (isDownloading) {
                    break
                }
                
                // 等待指定间隔
                delay(STATUS_CHECK_INTERVAL)
                
                // 增加检查计数
                checkCount++
                
                Log.d("DataBackupViewModel", "执行第 $checkCount 次状态检查")
                
                // 查询备份状态
                try {
                    // 如果开始下载，停止状态检查
                    if (isDownloading) {
                        break
                    }
                    
                    val response = backupApi.getBackupStatus()
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.isSuccess()) {
                            val status = apiResponse.data?.status ?: ""
                            val taskId = apiResponse.data?.taskId ?: "-1"
                            val taskStatus = BackupTaskStatus.valueOf(status)

                            // 如果备份已完成或失败，停止定时任务
                            if (taskStatus == BackupTaskStatus.COMPLETED) {
                                _backupState.value = BackupState.Ready(
                                    taskId = taskId,
                                    hasBackupFile = true,
                                    taskStatus = BackupTaskStatus.COMPLETED.name
                                )

                                // 通知用户备份已完成
                                _actionEvent.value = ActionEvent.ShowDialog(
                                    title = "备份完成",
                                    message = "您的数据已成功备份，可以点击下载按钮获取备份文件。",
                                    confirmAction = { clearActionEvent() },
                                    cancelAction = { clearActionEvent() }
                                )

                                // 停止检查任务
                                break

                            } else if (taskStatus == BackupTaskStatus.FAILED) {
                                _backupState.value = BackupState.Error("备份任务失败")
                                // 停止检查任务
                                break

                            } else if (taskStatus != BackupTaskStatus.PROCESSING) {
                                // 如果不是处理中状态，也停止检查
                                _backupState.value = BackupState.Ready(
                                    hasBackupFile = false,
                                    taskStatus = taskStatus.name
                                )
                                break
                            }

                            // 如果仍在处理中，继续下一轮检查
                            Log.d("DataBackupViewModel", "备份仍在进行中...")
                        } else {
                            Log.e("DataBackupViewModel", "API 返回错误: ${apiResponse.message}")
                            // 如果API返回错误，尝试下一轮检查
                        }
                    } else {
                        Log.e("DataBackupViewModel", "状态检查请求失败: ${response.message()}")
                        // 请求失败，尝试下一轮检查
                    }
                } catch (e: Exception) {
                    Log.e("DataBackupViewModel", "状态检查异常", e)
                    // 发生异常，尝试下一轮检查
                }
            }

            // 如果达到最大检查次数还没完成，显示超时信息
            if (checkCount >= MAX_CHECK_COUNT) {
                _backupState.value = BackupState.Error("备份任务超时，请稍后检查状态")
            }

            // 任务完成，清空引用
            statusCheckJob = null
        }
    }

    /**
     * 停止状态检查任务
     */
    private fun stopStatusCheckTask() {
        statusCheckJob?.cancel()
        statusCheckJob = null
    }

    // 确保在 ViewModel 被清理时取消任务
    override fun onCleared() {
        super.onCleared()
        stopStatusCheckTask()
    }


    /**
     * 下载备份文件
     */
    fun downloadBackupFile(taskId: String) {
        viewModelScope.launch {
            try {
                // 停止状态查询任务
                stopStatusCheckTask()

                // 设置下载状态
                isDownloading = true
                _backupState.value = BackupState.Downloading(taskId, 0f)

                val fileName = generateBackupFileName()
                
                // 触发请求保存位置事件
                _actionEvent.value = ActionEvent.RequestSaveLocation(
                    fileName = fileName,
                    taskId = taskId,
                    mimeType = "application/zip"
                )
            } catch (e: Exception) {
                Log.e("DataBackupViewModel", "下载准备失败", e)
                _backupState.value = BackupState.Error(e.message ?: "下载备份文件失败")
                // 重新启动状态查询任务
                startStatusCheckTask()
                isDownloading = false
            }
        }
    }
    
    /**
     * 保存备份文件到指定位置
     */
    fun saveBackupFile(taskId: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 构建下载 URL
                val baseUrl = getBaseUrl()
                val downloadUrl = "${baseUrl}/api/backup/download/${taskId}"
                
                Log.d("DataBackupViewModel", "准备下载文件: $downloadUrl")
                
                // 在主线程更新UI状态 - 初始进度0%
                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Downloading(taskId, 0f)
                }
                
                // 获取认证 token
                val authToken = getAuthToken()
                val headers = mutableMapOf<String, String>()
                if (authToken.isNotEmpty()) {
                    headers["Authorization"] = "Bearer $authToken"
                }
                
                // 从网络下载并写入所选位置
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // 创建网络请求
                    val client = OkHttpClient.Builder().build()
                    val request = okhttp3.Request.Builder()
                        .url(downloadUrl)
                        .apply {
                            headers.forEach { (key, value) -> 
                                this.addHeader(key, value)
                            }
                        }
                        .build()
                    
                    // 执行请求 - 更新进度到10%表示请求已发出
                    withContext(Dispatchers.Main) {
                        _backupState.value = BackupState.Downloading(taskId, 0.1f)
                    }
                    
                    // 执行请求
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("服务器响应错误: ${response.code}")
                    }
                    
                    // 获取响应体 - 更新进度到20%表示收到响应
                    withContext(Dispatchers.Main) {
                        _backupState.value = BackupState.Downloading(taskId, 0.2f)
                    }
                    
                    // 获取文件大小
                    val contentLength = response.body?.contentLength() ?: -1
                    var bytesRead: Int
                    var totalBytesRead: Long = 0
                    val buffer = ByteArray(8192) // 8KB buffer
                    
                    // 将响应内容写入文件
                    response.body?.byteStream()?.use { inputStream ->
                        // 开始读写操作 - 更新进度到30%
                        withContext(Dispatchers.Main) {
                            _backupState.value = BackupState.Downloading(taskId, 0.3f)
                        }
                        
                        var lastProgressUpdate = System.currentTimeMillis()
                        val progressUpdateInterval = 200L // 200ms更新一次UI，避免太频繁
                        
                        // 逐块读取并写入文件
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 计算下载进度
                            val currentTime = System.currentTimeMillis()
                            if (contentLength > 0 && currentTime - lastProgressUpdate > progressUpdateInterval) {
                                // 将30%-90%的范围分配给实际下载过程
                                val downloadProgress = totalBytesRead.toFloat() / contentLength
                                val overallProgress = 0.3f + (downloadProgress * 0.6f)
                                
                                withContext(Dispatchers.Main) {
                                    _backupState.value = BackupState.Downloading(taskId, overallProgress)
                                }
                                
                                lastProgressUpdate = currentTime
                            }
                        }
                    } ?: throw Exception("下载内容为空")
                    
                    // 下载完成，开始处理文件 - 更新进度到90%
                    withContext(Dispatchers.Main) {
                        _backupState.value = BackupState.Downloading(taskId, 0.9f)
                    }
                    
                    // 获取文件信息并保存
                    saveBackupFileInfo(uri, taskId)

                    // 通知服务器下载已完成 - 更新进度到95%
                    withContext(Dispatchers.Main) {
                        _backupState.value = BackupState.Downloading(taskId, 0.95f)
                    }
                    
                    notifyServerDownloadComplete(taskId)
                    
                    // 更新状态为下载完成并通知用户 - 更新进度到100%
                    withContext(Dispatchers.Main) {
                        _backupState.value = BackupState.Downloading(taskId, 1.0f)
                        
                        // 短暂延迟以显示100%完成状态
                        delay(500)
                        
                        // 重置下载状态
                        isDownloading = false
                        
                        // 通知下载完成
                        notifyDownloadComplete()
                        
                        // 重新启动状态查询
                        startStatusCheckTask()
                    }
                } ?: throw Exception("无法创建输出流")
            } catch (e: Exception) {
                Log.e("DataBackupViewModel", "下载失败", e)
                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Error(e.message ?: "下载备份文件失败")
                    
                    // 重置下载状态
                    isDownloading = false
                    
                    // 重新启动状态查询
                    startStatusCheckTask()
                }
            }
        }
    }
    
    /**
     * 保存备份文件信息
     */
    private suspend fun saveBackupFileInfo(uri: Uri, taskId: String) {
        // 获取文件信息
        val fileName = uri.lastPathSegment ?: generateBackupFileName()
        val filePath = getFilePathFromUri(context, uri) ?: "未知路径"
        val fileSize = getFileSizeFromUri(context, uri)
        val downloadDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
            java.util.Locale.getDefault()).format(java.util.Date())
        
        // 创建新的备份文件信息对象
        val newFileInfo = BackupFileInfo(
            id = System.currentTimeMillis().toString(), // 使用时间戳作为唯一ID
            fileName = fileName,
            fileSize = fileSize,
            downloadDate = downloadDate,
            filePath = filePath
        )
        
        // 保存到 SharedPreferences
        fileManager.addBackupFile(newFileInfo)
        
        // 更新文件列表状态
        loadBackupFiles()
    }
    
    /**
     * 通知服务器下载已完成
     */
    private suspend fun notifyServerDownloadComplete(taskId: String) {
        try {
            Log.d("DataBackupViewModel", "通知服务器下载已完成: $taskId")
            val completeResponse = backupApi.reportDownloadComplete(taskId)
            if (completeResponse.isSuccessful) {
                Log.d("DataBackupViewModel", "服务器已确认下载完成")
            } else {
                Log.w("DataBackupViewModel", "通知服务器下载完成失败: ${completeResponse.message()}")
            }
        } catch (e: Exception) {
            // 这里只记录错误但不影响用户体验
            Log.e("DataBackupViewModel", "通知服务器下载完成时出错", e)
        }
    }
    
    /**
     * 通知用户下载完成
     */
    private suspend fun notifyDownloadComplete() {
        withContext(Dispatchers.Main) {
            _backupState.value = BackupState.Ready(
                taskId = "",
                hasBackupFile = false,
                taskStatus = BackupTaskStatus.EMPTY.name
            )
            
            // 通知用户下载完成
            _actionEvent.value = ActionEvent.ShowDialog(
                title = "下载完成",
                message = "备份文件已成功保存",
                confirmAction = { clearActionEvent() },
                cancelAction = { clearActionEvent() }
            )
        }
    }


    /**
     * 清除事件
     */
    fun clearActionEvent() {
        _actionEvent.value = null
    }

    /**
     * 删除备份文件
     */
    fun deleteBackupFile(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 检查权限
                if (!checkStoragePermissions()) {
                    withContext(Dispatchers.Main) {
                        // 提示用户需要权限
                        _actionEvent.value = ActionEvent.ShowDialog(
                            title = "需要存储权限",
                            message = "删除文件需要存储访问权限，请授予权限后重试",
                            confirmAction = { 
                                clearActionEvent()
                                requestStoragePermissions() 
                            },
                            cancelAction = { clearActionEvent() }
                        )
                    }
                    return@launch
                }

                // 获取文件信息
                val fileToDelete = fileManager.getBackupFiles().find { it.id == fileId }
                
                if (fileToDelete != null) {
                    // 记录将要删除的文件路径
                    Log.d("DataBackupViewModel", "尝试删除文件: ${fileToDelete.filePath}")
                    
                    // 检查文件是否存在
                    val file = File(fileToDelete.filePath)
                    if (!file.exists()) {
                        Log.w("DataBackupViewModel", "文件不存在: ${fileToDelete.filePath}")
                        // 文件不存在但仍从记录中删除
                        fileManager.removeBackupFile(fileId)
                        loadBackupFiles()
                        return@launch
                    }
                    
                    // 检查文件权限
                    val canRead = file.canRead()
                    val canWrite = file.canWrite()
                    Log.d("DataBackupViewModel", "文件权限 - 可读: $canRead, 可写: $canWrite")
                    
                    // 尝试使用 SAF 删除文件 (如果文件路径是 content:// URI)
                    var deleted = false
                    try {
                        if (fileToDelete.filePath.startsWith("content://")) {
                            val uri = Uri.parse(fileToDelete.filePath)
                            deleted = context.contentResolver.delete(uri, null, null) > 0
                            Log.d("DataBackupViewModel", "通过 ContentResolver 删除文件: $deleted")
                        }
                    } catch (e: Exception) {
                        Log.e("DataBackupViewModel", "通过 ContentResolver 删除失败", e)
                    }
                    
                    // 如果上面的方法失败，尝试传统删除方式
                    if (!deleted) {
                        deleted = file.delete()
                        Log.d("DataBackupViewModel", "通过 File API 删除文件: $deleted")
                    }
                    
                    // 检查删除后文件是否仍存在
                    val stillExists = file.exists()
                    Log.d("DataBackupViewModel", "删除后文件是否仍存在: $stillExists")
                    
                    // 如果删除成功或文件不存在，从记录中移除
                    if (deleted || !stillExists) {
                        fileManager.removeBackupFile(fileId)
                        loadBackupFiles()
                        
                        // 主线程中更新 UI
                        withContext(Dispatchers.Main) {
                            _actionEvent.value = ActionEvent.ShowDialog(
                                title = "删除成功",
                                message = "备份文件已成功删除",
                                confirmAction = { clearActionEvent() },
                                cancelAction = { clearActionEvent() }
                            )
                        }
                    } else {
                        // 尝试获取更多信息
                        val parentDir = file.parentFile
                        val parentExists = parentDir?.exists() ?: false
                        val parentCanWrite = parentDir?.canWrite() ?: false
                        
                        Log.w("DataBackupViewModel", "删除失败 - 父目录存在: $parentExists, 父目录可写: $parentCanWrite")
                        
                        withContext(Dispatchers.Main) {
                            _actionEvent.value = ActionEvent.ShowDialog(
                                title = "删除失败",
                                message = "无法删除文件，可能是权限不足。是否请求权限？",
                                confirmAction = { 
                                    clearActionEvent()
                                    requestStoragePermissions() 
                                },
                                cancelAction = { clearActionEvent() }
                            )
                        }
                    }
                } else {
                    Log.w("DataBackupViewModel", "未找到要删除的文件记录: $fileId")
                    withContext(Dispatchers.Main) {
                        _backupState.value = BackupState.Error("未找到要删除的文件记录")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBackupViewModel", "删除备份文件失败", e)
                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Error("删除失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 请求权限事件
     */
    fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上，需要请求 MANAGE_EXTERNAL_STORAGE 权限
            _permissionEvent.value = PermissionEvent.RequestManageAllFilesPermission
        } else {
            // Android 10 及以下
            _permissionEvent.value = PermissionEvent.RequestStoragePermission
        }
    }

    /**
     * 检查存储权限
     */
    fun checkStoragePermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上
            return Environment.isExternalStorageManager()
        } else {
            // Android 10 及以下
            val readPermission = ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val writePermission = ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            return readPermission == PackageManager.PERMISSION_GRANTED &&
                   writePermission == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 清除权限事件
     */
    fun clearPermissionEvent() {
        _permissionEvent.value = null
    }
    
    /**
     * 获取基础 URL
     */
    private fun getBaseUrl(): String {
        // 可以从BuildConfig或其他配置获取
        return com.wy.diary.BuildConfig.API_BASE_URL 
    }
    
    /**
     * 获取认证 token
     */
    private suspend fun getAuthToken(): String {
        return appRepository.getToken()
    }
    
    /**
     * 生成标准格式的备份文件名
     */
    private fun generateBackupFileName(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val currentDate = dateFormat.format(java.util.Date())
        return "backup_$currentDate.zip"
    }

    // 定义权限事件类型 - 添加到 ActionEvent 或创建新的类
    sealed class PermissionEvent {
        object RequestStoragePermission : PermissionEvent()
        object RequestManageAllFilesPermission : PermissionEvent() // 用于 Android 11+
    }
}