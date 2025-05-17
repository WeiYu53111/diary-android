package com.wy.diary.api

import com.wy.diary.model.ApiResponse
import com.wy.diary.model.BackupStartResponse
import com.wy.diary.model.BackupStatusResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BackupApi {
    /**
     * 获取备份状态
     * 返回是否有正在运行的备份任务以及上次备份时间
     */
    @GET("api/backup/status")
    suspend fun getBackupStatus(): Response<ApiResponse<BackupStatusResponse>>

    /**
     * 启动备份任务
     */
    @POST("api/backup/user/start")
    suspend fun startBackup(): Response<ApiResponse<BackupStartResponse>>

    /**
     * 通知服务器备份文件已下载完成
     * @param taskId 任务ID
     * @return 操作结果
     */
    @GET("api/backup/complete/{taskId}")
    suspend fun reportDownloadComplete(@Path("taskId") taskId: String): Response<ApiResponse<Boolean>>
    
}