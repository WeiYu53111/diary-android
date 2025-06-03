package com.wy.diary.data.model


/**
 * 日期信息数据类
 *
 * 用于表示日记或其他记录的日期相关信息。
 */
data class DateInfo(
    val createTime: String, // 例如："2023-10-26 10:30:00" - 记录创建的精确时间
    val logTime: String,    // 例如："10-26" 或 "2023-10-26" - 日志记录的日期
    val logWeek: String,    // 例如："星期四" - 日志记录的星期
    val logLunar: String    // 例如："九月十二" - 日志记录的农历日期
)