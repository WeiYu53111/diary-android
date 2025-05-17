package com.wy.diary.api

import com.wy.diary.model.ApiResponse
import com.wy.diary.model.DiaryIdResponse
import com.wy.diary.model.DiaryListPagedData
import com.wy.diary.model.DiaryRequest
import com.wy.diary.model.ImageUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

}