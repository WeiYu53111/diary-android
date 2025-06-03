package com.wy.diary.data.repository

import android.content.Context
import android.net.Uri
import com.wy.diary.data.model.DiaryListPagedData
import com.wy.diary.data.model.DiaryRequest
import retrofit2.http.Query

interface DiaryRepository {

    suspend fun getDiaryId(): Result<String>

    suspend fun uploadImage(context: Context, photoUri: Uri, diaryId: String):Result<String>

    suspend fun saveDiary(request: DiaryRequest):Result<String>

    suspend fun getDiaryList(pageIndex: Int,pageSize: Int): Result<DiaryListPagedData>

    suspend fun deleteDiary(diaryId: String,createTime:String): Result<Boolean>
}