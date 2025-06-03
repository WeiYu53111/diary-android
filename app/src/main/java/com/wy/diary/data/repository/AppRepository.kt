package com.wy.diary.data.repository

interface AppRepository {
    suspend fun saveLoginStatus(value:Boolean)
    suspend fun getLoginStatus(): Boolean
    suspend fun saveToken(value:String)
    suspend fun getToken(): String
}