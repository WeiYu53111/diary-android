package com.wy.diary.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wy.diary.data.model.BackupFileInfo

/**
 * 备份文件信息管理器，负责备份文件信息的存储和读取
 */
class BackupFileManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("backup_files_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_BACKUP_FILES = "backup_files"
        private const val TAG = "BackupFileManager"
    }
    
    /**
     * 获取所有已下载的备份文件信息
     */
    fun getBackupFiles(): List<BackupFileInfo> {
        val json = sharedPreferences.getString(KEY_BACKUP_FILES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BackupFileInfo>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "读取备份文件信息失败", e)
            emptyList()
        }
    }
    
    /**
     * 添加新的备份文件信息
     */
    fun addBackupFile(fileInfo: BackupFileInfo) {
        val files = getBackupFiles().toMutableList()
        // 检查是否已存在相同ID的文件，如果存在则替换
        val existingIndex = files.indexOfFirst { it.id == fileInfo.id }
        if (existingIndex >= 0) {
            files[existingIndex] = fileInfo
        } else {
            files.add(fileInfo)
        }
        saveBackupFiles(files)
    }
    
    /**
     * 删除备份文件信息
     */
    fun removeBackupFile(fileId: String): Boolean {
        val files = getBackupFiles().toMutableList()
        val initialSize = files.size
        files.removeAll { it.id == fileId }
        val removed = initialSize > files.size
        if (removed) {
            saveBackupFiles(files)
        }
        return removed
    }
    
    /**
     * 更新备份文件信息
     */
    fun updateBackupFile(fileInfo: BackupFileInfo) {
        val files = getBackupFiles().toMutableList()
        val index = files.indexOfFirst { it.id == fileInfo.id }
        if (index >= 0) {
            files[index] = fileInfo
            saveBackupFiles(files)
        }
    }
    
    /**
     * 清空所有备份文件信息
     */
    fun clearBackupFiles() {
        sharedPreferences.edit().remove(KEY_BACKUP_FILES).apply()
    }
    
    /**
     * 保存备份文件列表
     */
    private fun saveBackupFiles(files: List<BackupFileInfo>) {
        try {
            val json = gson.toJson(files)
            sharedPreferences.edit().putString(KEY_BACKUP_FILES, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "保存备份文件信息失败", e)
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024f)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024f * 1024f))
            else -> String.format("%.1f GB", size / (1024f * 1024f * 1024f))
        }
    }

    /**
     * 判断是否是外部存储文档
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * 判断是否是下载文档
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * 判断是否是媒体文档
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}