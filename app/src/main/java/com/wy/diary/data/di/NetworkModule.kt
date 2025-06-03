package com.wy.diary.data.di

import com.wy.diary.data.remote.AuthInterceptor
import com.wy.diary.data.remote.BackupApi
import com.wy.diary.data.remote.DiaryService
import com.wy.diary.data.remote.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val baseClient = RetrofitClient.getOkHttpClient().newBuilder()
        // 添加认证拦截器
        baseClient.addInterceptor(authInterceptor)
        return baseClient.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(RetrofitClient.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDiaryService(retrofit: Retrofit): DiaryService {
        return retrofit.create(DiaryService::class.java)
    }

    @Provides
    @Singleton
    fun provideBackupApi(retrofit: Retrofit): BackupApi {
        return retrofit.create(BackupApi::class.java)
    }


}