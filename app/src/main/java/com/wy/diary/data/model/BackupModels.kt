package com.wy.diary.data.model

/**
 * 备份任务状态枚举
 * @property displayName 状态的显示名称
 */
enum class BackupTaskStatus(val description: String) {
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    EMPTY("空"),
    FAILED("失败");

    /**
     * 创建失败状态并附加失败原因
     * @param errorMessage 失败原因
     * @return 包含失败原因的状态字符串
     */
    companion object {
        fun failedWithReason(errorMessage: String): String {
            return "${FAILED.name}: $errorMessage"
        }
    }
}



sealed class BackupState {
    object Loading : BackupState()
    object HasRunningTask : BackupState()
    data class Ready(
        val taskId: String = "",
        val hasBackupFile: Boolean = false,
        val lastBackupTime: String = "",
        val taskStatus: String = ""
    ) : BackupState()
    data class Error(val errorMessage: String) : BackupState()
    data class Downloading(val taskId: String, val progress: Float = 0f) : BackupState() // 新增下载状态
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
    data class RequestSaveLocation(
        val fileName: String,
        val taskId: String,
        val mimeType: String
    ) : ActionEvent()
    
    data class ShowDialog(
        val title: String,
        val message: String,
        val confirmAction: () -> Unit,
        val cancelAction: () -> Unit
    ) : ActionEvent()
}