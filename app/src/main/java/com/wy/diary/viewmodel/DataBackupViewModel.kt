package com.wy.diary.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wy.diary.api.RetrofitClient
import com.wy.diary.api.BackupApi
import com.wy.diary.model.ApiResponse
import com.wy.diary.model.BackupStatusResponse
import com.wy.diary.model.BackupTaskStatus
import com.wy.diary.util.BackupFileManager
import com.wy.diary.util.FileUtils.getFilePathFromUri
import com.wy.diary.util.FileUtils.getFileSizeFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File

sealed class BackupState {
    object Loading : BackupState()
    object HasRunningTask : BackupState()
    data class Ready(
        val taskId: String = "",
        val taskStatus: String = "",
        val hasBackupFile: Boolean = false,  // 是否有可下载的备份文件
        val lastBackupTime: String = ""      // 最后一次备份时间字段
    ) : BackupState()

    data class Error(val message: String) : BackupState()
}

// 添加表示备份文件信息的数据类
data class BackupFileInfo(
    val id: String,
    val fileName: String,
    val fileSize: String,
    val downloadDate: String,
    val filePath: String
)

sealed class ActionEvent {
    data class ShowDialog(
        val title: String,
        val message: String,
        val confirmAction: () -> Unit,
        val cancelAction: () -> Unit
    ) : ActionEvent()
    
    data class RequestSaveLocation(
        val fileName: String,
        val taskId: String,
        val mimeType: String = "application/zip"
    ) : ActionEvent()
}

interface IDataBackupViewModel {
    val backupState: StateFlow<BackupState>
    val backupFiles: StateFlow<List<BackupFileInfo>> // 添加备份文件状态流
    fun checkBackupStatus()
    fun startBackup()
    fun downloadBackupFile(taskId: String)
    fun deleteBackupFile(fileId: String)
}

// 修改为 AndroidViewModel 以获取应用级 Context
open class DataBackupViewModel(application: Application) : AndroidViewModel(application), IDataBackupViewModel {

    private val backupApi = RetrofitClient.createService(BackupApi::class.java)
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Loading)
    override val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _actionEvent = MutableStateFlow<ActionEvent?>(null)
    val actionEvent: StateFlow<ActionEvent?> = _actionEvent.asStateFlow()
    
    // 添加文件管理器
    private val fileManager = BackupFileManager(application)
    
    // 添加文件列表状态流
    private val _backupFiles = MutableStateFlow<List<BackupFileInfo>>(emptyList())
    override val backupFiles: StateFlow<List<BackupFileInfo>> = _backupFiles.asStateFlow()
    
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

    override fun checkBackupStatus() {
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
                                taskStatus = taskStatus?.name ?: "未知"
                            )
                        }

                    } else {
                        _backupState.value =
                            BackupState.Error("获取备份状态失败：${apiResponse.message}")
                    }
                } else {
                    _backupState.value = BackupState.Error("获取备份状态失败：${response.message()}")
                }
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "网络请求异常")
            }
        }
    }

    override fun startBackup() {
        viewModelScope.launch {
            try {
                val response = backupApi.startBackup()
                if (response.isSuccessful) {
                    _backupState.value = BackupState.HasRunningTask
                } else {
                    _backupState.value = BackupState.Error("启动备份失败：${response.message()}")
                }
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "备份启动失败")
            }
        }
    }

    // 下载备份文件
    override fun downloadBackupFile(taskId: String) {
        viewModelScope.launch {
            try {
                val fileName = "diary_backup_${taskId}_${System.currentTimeMillis()}.zip"
                
                // 触发请求保存位置事件
                _actionEvent.value = ActionEvent.RequestSaveLocation(
                    fileName = fileName,
                    taskId = taskId,
                    mimeType = "application/zip"
                )
            } catch (e: Exception) {
                Log.e("DataBackupViewModel", "下载准备失败", e)
                _backupState.value = BackupState.Error(e.message ?: "下载备份文件失败")
            }
        }
    }

    private fun actuallyStartDownload(taskId: String) {
        viewModelScope.launch {
            try {
                // 使用 AndroidViewModel 提供的 getApplication 获取 Context
                val ctx = getApplication<Application>()
                
                // 构建下载URL
                val baseUrl = RetrofitClient.BASE_URL
                val downloadUrl = "${baseUrl}/api/backup/download/${taskId}"
                
                Log.d("DataBackupViewModel", "准备下载文件: $downloadUrl")
                
                // 创建下载请求
                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle("日记备份文件")
                    .setDescription("正在下载您的日记备份文件")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setMimeType("application/zip")
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "diary_backup_${taskId}_${System.currentTimeMillis()}.zip"
                    )
                
                // 添加认证头（如果需要）
                val authToken = RetrofitClient.getAuthToken()
                if (authToken.isNotEmpty()) {
                    request.addRequestHeader("Authorization", "Bearer $authToken")
                    Log.d("DataBackupViewModel", "添加认证头: Bearer $authToken")
                }
                
                // 获取下载管理器并开始下载
                val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)
                
                Log.d("DataBackupViewModel", "下载已开始，下载ID: $downloadId")
                
                // 通知用户下载已开始
                _backupState.value = BackupState.Ready(
                    taskId = taskId,
                    hasBackupFile = true,
                    taskStatus = "下载中" // 或使用枚举值
                )
            } catch (e: Exception) {
                Log.e("DataBackupViewModel", "下载失败", e)
                _backupState.value = BackupState.Error(e.message ?: "下载备份文件失败")
            }
        }
    }

    // 修改 saveBackupFile 方法
    fun saveBackupFile(taskId: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 使用 AndroidViewModel 提供的 getApplication 获取 Context
                val ctx = getApplication<Application>()
                
                // 构建下载 URL
                val baseUrl = RetrofitClient.BASE_URL
                val downloadUrl = "${baseUrl}/api/backup/download/${taskId}"
                
                Log.d("DataBackupViewModel", "准备下载文件: $downloadUrl")
                
                // 在主线程更新UI状态
                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Ready(
                        taskId = taskId, 
                        hasBackupFile = true,
                        taskStatus = "下载中"
                    )
                }
                
                // 使用 Retrofit 或自定义下载逻辑
                val authToken = RetrofitClient.getAuthToken()
                val headers = mutableMapOf<String, String>()
                if (authToken.isNotEmpty()) {
                    headers["Authorization"] = "Bearer $authToken"
                }
                
                // 从网络下载并写入所选位置
                ctx.contentResolver.openOutputStream(uri)?.use { outputStream ->
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
                    
                    // 执行请求
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("服务器响应错误: ${response.code}")
                    }
                    
                    // 将响应内容写入文件
                    response.body?.byteStream()?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    } ?: throw Exception("下载内容为空")
                    
                    // 获取文件信息
                    val fileName = uri.lastPathSegment ?: "backup_${System.currentTimeMillis()}.zip"
                    val filePath = getFilePathFromUri(ctx, uri) ?: "未知路径"
                    val fileSize = getFileSizeFromUri(ctx, uri)
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

                    // 通知服务器下载已完成
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
                    
                    // 更新状态为下载完成
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
            } catch (e: Exception) {
                Log.e("DataBackupViewModel", "下载失败", e)
                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Error(e.message ?: "下载备份文件失败")
                }
            }
        }
    }
    
    // 修改删除备份文件方法
    override fun deleteBackupFile(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 获取文件信息
                val fileToDelete = fileManager.getBackupFiles().find { it.id == fileId }
                
                if (fileToDelete != null) {
                    // 物理删除文件
                    val file = File(fileToDelete.filePath)
                    val deleted = file.delete()
                    
                    // 如果删除成功或文件不存在，从记录中移除
                    if (deleted || !file.exists()) {
                        fileManager.removeBackupFile(fileId)
                        
                        // 重新加载文件列表
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
                        withContext(Dispatchers.Main) {
                            _backupState.value = BackupState.Error("删除文件失败")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBackupViewModel", "删除备份文件失败", e)
                withContext(Dispatchers.Main) {
                    _backupState.value = BackupState.Error(e.message ?: "删除备份文件失败")
                }
            }
        }
    }

    // 提供清除事件的方法
    fun clearActionEvent() {
        _actionEvent.value = null
    }

    // 为 ViewModel 提供自定义工厂
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DataBackupViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DataBackupViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }




}