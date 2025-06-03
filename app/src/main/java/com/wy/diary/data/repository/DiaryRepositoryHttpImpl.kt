package com.wy.diary.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import cn.hutool.core.date.ChineseDate
import com.wy.diary.data.model.DateInfo
import com.wy.diary.data.model.DeleteDiaryRequest
import com.wy.diary.data.model.DiaryListPagedData
import com.wy.diary.data.model.DiaryRequest
import com.wy.diary.data.remote.DiaryService
import com.wy.diary.data.remote.RetrofitClient
import com.wy.diary.data.remote.getDataSafely
import com.wy.diary.data.remote.getErrorMessage
import com.wy.diary.data.remote.isApiCallSuccess
import com.wy.diary.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * 日记仓库 - 处理与后端API的所有交互
 */
class DiaryRepositoryHttpImpl @Inject constructor(
    private val diaryService: DiaryService,
    private val appRepository: AppRepository
) : DiaryRepository {

    /**
     * 获取日记ID
     */
    override suspend fun getDiaryId(): Result<String> {
        Log.d("DiaryRepository", "正在获取日记ID...")

        return try {
            val response = diaryService.getDiaryId()
            Log.d("DiaryRepository", "获取日记ID响应: ${response.code()}, body: ${response.body()}")

            if (!response.isApiCallSuccess()) {
                Log.e("DiaryRepository", "获取日记ID失败: ${response.getErrorMessage()}")
                Result.failure(Exception("获取日记ID失败: ${response.getErrorMessage()}"))
            } else {
                val diaryId = response.getDataSafely()?.diaryId
                if (diaryId != null) {
                    Result.success(diaryId)
                } else {
                    Log.e("DiaryRepository", "获取日记ID失败: 返回数据为空")
                    Result.failure(Exception("获取日记ID失败: 返回数据为空"))
                }
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "获取日记ID异常", e)
            Result.failure(e)
        }
    }

    /**
     * 上传单张图片
     */
    override suspend fun uploadImage(
        context: Context,
        photoUri: Uri,
        diaryId: String
    ): Result<String> {
        Log.d("DiaryRepository", "处理图片: $photoUri")

        return try {
            // 将 Uri 转换为文件
            val file = withContext(Dispatchers.IO) {
                FileUtils.uriToFile(context, photoUri)
            }
            Log.d(
                "DiaryRepository",
                "转换后的文件: ${file?.absolutePath}, 大小: ${file?.length()} bytes"
            )

            if (file == null || !file.exists()) {
                Log.e("DiaryRepository", "文件不存在或无效")
                return Result.failure(Exception("文件无效"))
            }

            // 上传图片
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            Log.d("DiaryRepository", "开始上传文件: ${file.name}")
            val uploadResponse = diaryService.uploadImage(body, diaryId)
            Log.d(
                "DiaryRepository",
                "上传响应: ${uploadResponse.code()}, body: ${uploadResponse.body()}"
            )

            if (!uploadResponse.isApiCallSuccess()) {
                Log.e("DiaryRepository", "上传失败: ${uploadResponse.getErrorMessage()}")
                Result.failure(Exception("图片上传失败: ${uploadResponse.getErrorMessage()}"))
            } else {
                val imageUrl = uploadResponse.getDataSafely()?.url
                    ?: uploadResponse.getDataSafely()?.toString()

                Log.d("DiaryRepository", "获取到的图片URL: $imageUrl")

                if (imageUrl != null) {
                    Result.success(imageUrl)
                } else {
                    Log.e("DiaryRepository", "图片URL为空")
                    Result.failure(Exception("图片URL获取失败"))
                }
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "上传图片异常", e)
            Result.failure(e)
        }
    }

    /**
     * 保存日记
     */
    override suspend fun saveDiary(request: DiaryRequest): Result<String> {
        Log.d("DiaryRepository", "准备保存日记内容")

        return try {
            val saveResponse = diaryService.saveDiary(request)

            if (!saveResponse.isApiCallSuccess()) {
                Log.e("DiaryRepository", "保存日记失败: ${saveResponse.getErrorMessage()}")
                Result.failure(Exception("保存日记失败: ${saveResponse.getErrorMessage()}"))
            } else {
                val diaryId = saveResponse.getDataSafely()?.toString() ?: ""
                Result.success(diaryId)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "保存日记异常", e)
            Result.failure(e)
        }
    }

    override suspend fun getDiaryList(pageIndex: Int, pageSize: Int): Result<DiaryListPagedData> {
        Log.d("DiaryRepository", "正在获取日记列表: 页码=$pageIndex, 每页数量=$pageSize")

        return try {
            val response = diaryService.getDiaryList(pageIndex, pageSize)
            Log.d("DiaryRepository", "获取日记列表响应: ${response.code()}")

            if (!response.isApiCallSuccess()) {
                Log.e("DiaryRepository", "获取日记列表失败: ${response.getErrorMessage()}")
                Result.failure(Exception(response.getErrorMessage() ?: "获取列表失败"))
            } else {
                // 获取原始数据
                val originalData = response.getDataSafely() ?: DiaryListPagedData(
                    records = emptyList(),
                    totalCount = 0,
                    totalPages = 0,
                    hasNext = false
                )

                // 处理记录中的图片URL
                val processedRecords = originalData.records?.map { record ->
                    // 复制原始记录，但处理图片URLs
                    record.copy(
                        imageUrls = record.imageUrls?.map { imageUrl ->
                            processImageUrl(imageUrl)
                        }
                    )
                } ?: emptyList()

                // 创建处理后的数据对象
                val processedData = originalData.copy(
                    records = processedRecords
                )

                Log.d("DiaryRepository", "成功获取日记列表: ${processedData.records.size} 条记录")
                Result.success(processedData)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "获取日记列表异常", e)
            Result.failure(e)
        }
    }

    /**
     * 处理图片URL - 将相对路径转换为完整的URL
     */
    /**
     * 处理图片URL (添加需要的前缀和token)
     */
    suspend fun processImageUrl(imageUrl: String): String {
        return if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            val openid = appRepository.getToken()
            val processedUrl =
                "${RetrofitClient.BASE_URL}${RetrofitClient.IMAGE_API_PREFX}?id=${openid}&file=${imageUrl}"
            processedUrl
        } else {
            imageUrl
        }
    }

    /**
     * 获取当前日期信息
     */
    fun getCurrentDateInfo(): DateInfo {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val weekDayFormat = SimpleDateFormat("EEEE", Locale.CHINESE)
        val date = dateFormat.format(calendar.time)
        val weekday = weekDayFormat.format(calendar.time)

        val chineseDate = ChineseDate(calendar.time)
        val lunarDate = "${chineseDate.chineseMonthName}${chineseDate.chineseDay}"

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")  // 关键步骤：强制UTC时区
        val utcTime = sdf.format(Date())  // 输出真实的UTC时间

        return DateInfo(
            createTime = utcTime,
            logTime = date,
            logWeek = weekday,
            logLunar = lunarDate
        )
    }

    /**
     * 获取用户OpenID
     */
    fun getUserOpenId(): String {
        return runBlocking { appRepository.getToken() }
    }

    /**
     * 批量上传图片
     */
    suspend fun uploadImages(
        context: Context,
        photos: List<Uri>,
        diaryId: String
    ): Result<List<String>> {
        return try {
            val results = coroutineScope {
                photos.map { uri ->
                    async(Dispatchers.IO) {
                        uploadImage(context, uri, diaryId)
                    }
                }.awaitAll()
            }

            // 收集成功上传的图片URL和失败的错误信息
            val successUrls = mutableListOf<String>()
            val errorMessages = mutableListOf<String>()

            results.forEach { result ->
                // 使用安全的方法访问Result内容
                val url = result.getOrNull()
                val error = result.exceptionOrNull()

                if (url != null) {
                    successUrls.add(url)
                } else if (error != null) {
                    errorMessages.add(error.message ?: "未知错误")
                }
            }

            // 根据结果返回
            if (errorMessages.isNotEmpty()) {
                // 有失败的上传
                Result.failure(Exception("部分图片上传失败: ${errorMessages.joinToString(", ")}"))
            } else {
                // 全部上传成功
                Result.success(successUrls)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "批量上传图片异常", e)
            Result.failure(e)
        }
    }

    /**
     * 删除日记
     */
    override suspend fun deleteDiary(diaryId: String,createTime:String): Result<Boolean> {
        Log.d("DiaryRepository", "正在删除日记: ID=$diaryId")
        val createYear = createTime.replace("-","").substring(0,4)


        val requestData = DeleteDiaryRequest(diaryId = diaryId, createYear = createYear)
        return try {
            val response = diaryService.deleteDiary(requestData)
            Log.d("DiaryRepository", "删除日记响应: ${response.code()}")

            if (!response.isApiCallSuccess()) {
                Log.e("DiaryRepository", "删除日记失败: ${response.getErrorMessage()}")
                Result.failure(Exception(response.getErrorMessage() ?: "删除日记失败"))
            } else {
                Log.d("DiaryRepository", "日记删除成功")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "删除日记异常", e)
            Result.failure(e)
        }
    }
}