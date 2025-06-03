package com.wy.diary.data.repository

import com.wy.diary.data.model.UserInfo
import javax.inject.Inject

class UserInfoRepositoryFakeImpl @Inject constructor() : UserInfoRepository {
    override suspend fun getUserInfo(): Result<UserInfo> {
        return Result.success(UserInfo(
            diaryCount = 0,
            registerTime = "2025-05-30"
        ))
    }


}