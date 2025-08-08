package com.wy.diary.util

import com.wy.diary.data.model.DiaryItem
import com.wy.diary.data.model.DiaryRecord
import java.text.SimpleDateFormat
import java.util.*

/**
 * 缓存相关扩展函数
 */

/**
 * DiaryRecord 转换为 DiaryItem
 */
fun DiaryRecord.toDiaryItem(): DiaryItem {
    val contentPreview = editorContent?.let { content ->
        if (content.length > 100) {
            content.substring(0, 100) + "..."
        } else {
            content
        }
    } ?: ""

    val createTimeFormatted = createTime?.let { time ->
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINESE)
            val date = inputFormat.parse(time)
            date?.let { outputFormat.format(it) } ?: time
        } catch (e: Exception) {
            time
        }
    } ?: ""

    return DiaryItem(
        diaryId = diaryId,
        logTime = logTime ?: "",
        logWeek = logWeek ?: "",
        logLunar = logLunar,
        contentPreview = contentPreview,
        content = editorContent ?: "",
        images = imageUrls ?: emptyList(),
        createTimeFormatted = createTimeFormatted,
        address = address
    )
}

/**
 * DiaryRecord列表转换为DiaryItem列表
 */
fun List<DiaryRecord>.toDiaryItems(): List<DiaryItem> {
    return map { it.toDiaryItem() }
}