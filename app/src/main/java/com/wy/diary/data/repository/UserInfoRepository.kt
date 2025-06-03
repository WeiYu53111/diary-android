package com.wy.diary.data.repository

import com.wy.diary.data.model.UserInfo

interface UserInfoRepository {

   suspend fun getUserInfo():Result<UserInfo>
}