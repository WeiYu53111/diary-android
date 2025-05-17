package com.wy.diary.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wy.diary.api.DiaryService
import com.wy.diary.api.RetrofitClient
import com.wy.diary.api.isApiCallSuccess
import com.wy.diary.api.getErrorMessage
import com.wy.diary.api.getDataSafely
import com.wy.diary.model.DiaryRequest
import com.wy.diary.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.text.SimpleDateFormat
import java.util.*
import cn.hutool.core.date.ChineseDate

class DiaryViewModel : ViewModel() {
    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent
    
    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving
    
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError
    
    private val _address = MutableStateFlow("未选择地址")
    val address: StateFlow<String> = _address
    
    // API 服务
    private val diaryService = RetrofitClient.createService(DiaryService::class.java)
    
    fun updateEditorContent(content: String) {
        _editorContent.value = content
    }
    
    // 在处理 Uri 时，确保正确提取文件名
    fun addPhoto(uri: Uri) {
        val fileName = uri.lastPathSegment ?: uri.toString()
        // 移除可能的引号
        val cleanFileName = fileName.replace("\"", "")
        
        Log.d("DiaryViewModel", "Adding photo with original URI: $uri")
        Log.d("DiaryViewModel", "Extracted filename: $cleanFileName")
        
        _photos.update { currentPhotos -> currentPhotos + uri }
    }
    
    fun removePhoto(uri: Uri) {
        _photos.value = _photos.value.filter { it != uri }
    }
    
    fun updateAddress(newAddress: String) {
        _address.value = newAddress
    }
    
    fun clearContent() {
        _editorContent.value = ""
        _photos.value = emptyList()
        _address.value = "未选择地址"
    }
    
    fun saveDiary(context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            
            try {
                // 1. 获取日记ID
                Log.d("DiaryViewModel", "正在获取日记ID...")
                val idResponse = diaryService.getDiaryId()
                Log.d("DiaryViewModel", "获取日记ID响应: ${idResponse.code()}, body: ${idResponse.body()}")

//                if (!idResponse.isApiCallSuccess()) {
//                    throw Exception("获取日记ID失败: ${idResponse.getErrorMessage()}")
//                }

                val diaryId = idResponse.getDataSafely()?.diaryId
                    ?: throw Exception("获取日记ID失败: 返回数据为空")

                // 2. 上传图片
                Log.d("DiaryViewModel", "开始上传图片，共 ${_photos.value.size} 张")
                if (_photos.value.isEmpty()) {
                    Log.d("DiaryViewModel", "没有图片需要上传")
                }

                val imageUrls = mutableListOf<String>()
                for (photoUri in _photos.value) {
                    Log.d("DiaryViewModel", "处理图片: $photoUri")
                    
                    try {
                        // 将 Uri 转换为文件
                        val file = withContext(Dispatchers.IO) {
                            FileUtils.uriToFile(context, photoUri)
                        }
                        Log.d("DiaryViewModel", "转换后的文件: ${file?.absolutePath}, 大小: ${file?.length()} bytes")
                        
                        if (file == null || !file.exists()) {
                            Log.e("DiaryViewModel", "文件不存在或无效")
                            continue
                        }
                        
                        // 上传图片
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                        
                        Log.d("DiaryViewModel", "开始上传文件: ${file.name}")
                        val uploadResponse = diaryService.uploadImage(body, diaryId)
                        Log.d("DiaryViewModel", "上传响应: ${uploadResponse.code()}, body: ${uploadResponse.body()}")
                        
                        if (!uploadResponse.isApiCallSuccess()) {
                            Log.e("DiaryViewModel", "上传失败: ${uploadResponse.getErrorMessage()}")
                            throw Exception("图片上传失败: ${uploadResponse.getErrorMessage()}")
                        }
                        
                        val imageUrl = uploadResponse.getDataSafely()?.url 
                            ?: uploadResponse.getDataSafely()?.toString()
                        
                        Log.d("DiaryViewModel", "获取到的图片URL: $imageUrl")
                        
                        if (imageUrl == null) {
                            Log.e("DiaryViewModel", "图片URL为空")
                            throw Exception("图片URL获取失败")
                        }
                        
                        imageUrls.add(imageUrl)
                        Log.d("DiaryViewModel", "已添加图片URL，当前共: ${imageUrls.size}")
                    } catch (e: Exception) {
                        Log.e("DiaryViewModel", "处理图片时出错", e)
                        throw e
                    }
                }
                
                // 3. 保存日记内容
                Log.d("DiaryViewModel", "准备保存日记内容")
                val openId = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getString("openid", "") ?: ""
                
                // 获取当前日期、星期和农历
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val weekDayFormat = SimpleDateFormat("EEEE", Locale.CHINESE)
                val date = dateFormat.format(calendar.time)
                val weekday = weekDayFormat.format(calendar.time)
                
                val chineseDate = ChineseDate(calendar.time)
                val lunarDate = "${chineseDate.chineseMonthName}${chineseDate.chineseDay}"

                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")  // 关键步骤：强制UTC时区
                val utcTime =  sdf.format(Date())         // 输出真实的UTC时间

                val diaryRequest = DiaryRequest(
                    openId = openId,
                    editorContent = _editorContent.value,
                    createTime = utcTime,
                    logTime = date,
                    logWeek = weekday,
                    logLunar = lunarDate,
                    address = _address.value,
                    imageUrls = imageUrls,
                    diaryId = diaryId
                )
                
                val saveResponse = diaryService.saveDiary(diaryRequest)
                
                if (!saveResponse.isApiCallSuccess()) {
                    throw Exception("保存日记失败: ${saveResponse.getErrorMessage()}")
                }
                
                // 保存成功，清空内容
                clearContent()
                
            } catch (e: Exception) {
                _saveError.value = "保存失败: ${e.message}"
                
                // 可以添加回滚逻辑，如删除已上传的图片
            } finally {
                _isSaving.value = false
            }
        }
    }
}