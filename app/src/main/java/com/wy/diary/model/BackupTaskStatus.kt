package com.wy.diary.model

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