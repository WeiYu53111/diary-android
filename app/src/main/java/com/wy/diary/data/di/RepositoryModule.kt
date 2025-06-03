package com.wy.diary.data.di
// 创建 Hilt Module (例如: RepositoryModule.kt)
import com.wy.diary.data.repository.AppRepository
import com.wy.diary.data.repository.AppRepositoryLocalImpl
import com.wy.diary.data.repository.DiaryRepository
import com.wy.diary.data.repository.DiaryRepositoryHttpImpl
import com.wy.diary.data.repository.UserInfoRepository
import com.wy.diary.data.repository.UserInfoRepositoryFakeImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module // 标记这是一个 Hilt Module
@InstallIn(SingletonComponent::class) // 告诉 Hilt 这个 Module 应该安装到哪个组件中，这里是 Application 级别的单例组件
abstract class RepositoryModule { // 如果 Module 只包含抽象的 @Binds 方法，它可以是抽象类

    // 使用 @Binds 注解告诉 Hilt：
    // 当有人请求 UserRepository 接口时，请提供 UserRepositoryImpl 的实例。
    // 同时，使用 @Singleton 确保整个应用生命周期内 UserRepositoryImpl 只有一个实例。
    @Singleton
    @Binds
    abstract fun bindAppRepository(
        appRepositoryLocalImpl: AppRepositoryLocalImpl // Hilt 会自动注入 UserRepositoryImpl 的实例
    ): AppRepository


    // 使用 @Binds 注解告诉 Hilt：
    // 当有人请求 UserRepository 接口时，请提供 UserRepositoryImpl 的实例。
    // 同时，使用 @Singleton 确保整个应用生命周期内 UserRepositoryImpl 只有一个实例。
    @Singleton
    @Binds
    abstract fun bindDiaryRepository(
        diaryRepositoryHttpImpl: DiaryRepositoryHttpImpl // Hilt 会自动注入 UserRepositoryImpl 的实例
    ): DiaryRepository


    @Singleton
    @Binds
    abstract fun bindUserInfoRepository(
        impl: UserInfoRepositoryFakeImpl // Hilt 会自动注入 UserRepositoryImpl 的实例
    ): UserInfoRepository
}
