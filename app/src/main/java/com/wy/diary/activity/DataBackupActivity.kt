package com.wy.diary.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings  // 添加这行导入语句
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.wy.diary.R
import com.wy.diary.data.model.ActionEvent
import com.wy.diary.ui.screen.DataBackupScreen
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.DataBackupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DataBackupActivity : ComponentActivity() {
    
    // 使用 Hilt 注入 ViewModel
    private val viewModel: DataBackupViewModel by viewModels()
    
    // 添加文件创建启动器
    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>
    private var pendingTaskId: String? = null
    
    // 权限请求码
    private val REQUEST_STORAGE_PERMISSIONS = 1001
    private val REQUEST_MANAGE_ALL_FILES = 1002
    
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
        
        // 设置权限事件观察者
        setupPermissionObserver()
        
        setContent {
            DiaryAndroidTheme {
                DataBackupScreen(
                    backupState = viewModel.backupState,
                    backupFiles = viewModel.backupFiles,
                    onNavigateBack = { finish() },
                    checkBackUPStatusFun = {viewModel.checkBackupStatus()},
                    startBackupFun = {viewModel.startBackup()},
                    downloadBackupFileFun =  { taskId -> viewModel.downloadBackupFile(taskId)} ,
                    deleteBackupFileFun = {filedId -> viewModel.deleteBackupFile(filedId)}
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
    
    // 设置权限事件观察者
    private fun setupPermissionObserver() {
        lifecycleScope.launch {
            viewModel.permissionEvent.collect { event ->
                when (event) {
                    is DataBackupViewModel.PermissionEvent.RequestStoragePermission -> {
                        requestStoragePermissions()
                    }
                    is DataBackupViewModel.PermissionEvent.RequestManageAllFilesPermission -> {
                        requestManageAllFilesPermission()
                    }
                    null -> { /* 无操作 */ }
                }
            }
        }
    }

    // 请求存储权限
    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_STORAGE_PERMISSIONS
        )
    }

    // 请求管理所有文件权限
    private fun requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${applicationContext.packageName}")
                startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES)
            }
        }
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && 
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 权限已授予，可以继续操作
                Toast.makeText(this, "存储权限已获得，可以继续操作", Toast.LENGTH_SHORT).show()
            } else {
                // 提示用户权限被拒绝
                Toast.makeText(this, "需要存储权限才能删除文件", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearPermissionEvent()
        }
    }

    // 处理活动结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // 权限已授予，可以继续操作
                    Toast.makeText(this, "存储权限已获得，可以继续操作", Toast.LENGTH_SHORT).show()
                } else {
                    // 提示用户权限被拒绝
                    Toast.makeText(this, "需要存储权限才能删除文件", Toast.LENGTH_SHORT).show()
                }
            }
            viewModel.clearPermissionEvent()
        }
    }
    
    // 重写 finish() 方法以添加返回动画
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}