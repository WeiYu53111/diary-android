package com.wy.diary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 日记本地数据库实体
 */
@Entity(tableName = "diary_entries")
@TypeConverters(Converters::class)
data class DiaryEntity(
    @PrimaryKey
    val diaryId: String,
    val logTime: String?,
    val logWeek: String?,
    val logLunar: String?,
    val editorContent: String?,
    val imageUrls: List<String>?,
    val createTime: String?,
    val address: String?,
    val lastUpdateTime: Long = System.currentTimeMillis(), // 用于增量更新
    val isDeleted: Boolean = false // 软删除标记
)

/**
 * 缓存元数据实体 - 用于管理缓存状态
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey
    val key: String,
    val lastUpdateTime: Long,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val expiryTime: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24小时过期
)

/**
 * 类型转换器 - Room数据库需要
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}