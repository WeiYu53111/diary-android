package com.wy.diary.data.remote

import com.wy.diary.data.model.ApiResponse
import com.wy.diary.data.model.DeleteDiaryRequest
import com.wy.diary.data.model.DiaryIdResponse
import com.wy.diary.data.model.DiaryListPagedData
import com.wy.diary.data.model.DiaryRequest
import com.wy.diary.data.model.ImageUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface DiaryService {
    @GET("/api/diary/getDiaryId")
    suspend fun getDiaryId(): Response<ApiResponse<DiaryIdResponse>>
    
    @Multipart
    @POST("/api/images/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Query("diaryId") diaryId: String
    ): Response<ApiResponse<ImageUploadResponse>>
    
    @POST("/api/diary/save")
    suspend fun saveDiary(@Body request: DiaryRequest): Response<ApiResponse<Any>>


    @GET("/api/diary/list")
    suspend fun getDiaryList(
        @Query("pageIndex") pageIndex: Int,
        @Query("pageSize") pageSize: Int
    ): Response<ApiResponse<DiaryListPagedData>>

    @POST("api/diary/delete")
    suspend fun deleteDiary(@Body request: DeleteDiaryRequest): Response<ApiResponse<Boolean>>



}