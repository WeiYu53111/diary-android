# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

这是一个基于Android Kotlin的日记应用，使用现代Android开发技术栈：Jetpack Compose、MVVM架构、Hilt依赖注入、Retrofit网络请求、Room本地缓存等。应用支持写日记、上传图片、备份管理、本地缓存等功能。

## 开发环境设置

在开始开发前，必须完成以下配置：

1. 复制 `local.properties.template` 为 `local.properties`
2. 在 `local.properties` 中配置：
   - `sdk.dir`: Android SDK路径
   - `dev.open.id`: 开发环境OpenID
   - `prod.open.id`: 生产环境OpenID  
   - `api.debug.url`: 调试环境API地址 (默认: http://10.0.2.2:7080)
   - `api.release.url`: 生产环境API地址

## 常用命令

### 构建项目
```bash
./gradlew build
```

### 运行测试
```bash
./gradlew test                 # 单元测试
./gradlew connectedAndroidTest # Android设备测试
```

### 清理项目
```bash
./gradlew clean
```

### 构建APK
```bash
./gradlew assembleDebug   # Debug版本
./gradlew assembleRelease # Release版本
```

### Lint检查
```bash
./gradlew lint
```

## 代码架构

### MVVM + Repository + 缓存模式
- **View层**: Jetpack Compose UI + Activities
- **ViewModel层**: 处理UI状态和业务逻辑，使用 @HiltViewModel
- **Repository层**: 数据访问抽象层，统一本地缓存和远程数据源
- **Cache层**: Room本地数据库，支持Cache-First策略
- **Data层**: 网络API、数据模型、工具类

### 依赖注入 (Hilt)
- `DiaryApplication`: 使用 @HiltAndroidApp 标注的应用类
- `NetworkModule`: 提供网络相关依赖 (Retrofit, OkHttp, API服务)
- `DatabaseModule`: 提供Room数据库和DAO依赖
- `RepositoryModule`: 绑定Repository接口到带缓存的实现
- `WorkerModule`: 提供WorkManager和缓存后台任务依赖

### 关键目录结构
```
app/src/main/java/com/wy/diary/
├── activity/           # 活动类
├── components/         # 可复用UI组件
├── data/
│   ├── cache/         # 缓存管理器和Worker
│   ├── di/            # Hilt依赖注入模块
│   ├── local/         # Room数据库实体和DAO
│   ├── model/         # 数据模型
│   ├── remote/        # 网络API和拦截器
│   └── repository/    # Repository实现(网络+缓存)
├── ui/
│   ├── screen/        # Compose屏幕
│   └── theme/         # 主题配置
├── util/              # 工具类和扩展函数
└── viewmodel/         # ViewModel类
```

### 网络请求架构
- 使用Retrofit + OkHttp + Gson
- `AuthInterceptor`: 自动添加认证头
- `DiaryService`: 日记相关API
- `BackupApi`: 备份相关API
- 统一的错误处理和响应模型

### 状态管理
- ViewModels使用StateFlow管理UI状态
- Repository层返回Result类型处理成功/失败状态
- 使用协程处理异步操作

## 技术栈特点

### UI框架
- **Jetpack Compose**: 现代声明式UI框架
- **Material Design 3**: 遵循最新设计规范
- **传统View系统**: 部分复杂界面仍使用传统布局

### 图片处理
- **Coil**: Compose中的图片加载
- **Glide**: 传统View中的图片加载和缓存
- 支持圆形头像、图片预览等功能

### 数据存储
- **DataStore**: 替代SharedPreferences的现代存储方案
- **本地文件**: 用于备份文件管理

### 第三方集成
- **微信SDK**: 支持微信登录和分享
- **Hutool**: 中国农历计算等工具库

## 开发注意事项

### 敏感信息管理
- 所有API密钥和OpenID配置在local.properties中
- 不同构建类型使用不同的配置值
- local.properties文件不会提交到版本控制

### 构建配置
- 支持debug/release两种构建类型
- 可选的staging构建类型（当前注释状态）
- 使用version catalog (libs.versions.toml) 管理依赖版本

### 代码规范
- 使用Kotlin协程处理异步操作
- 遵循MVVM架构分层原则
- Repository接口与实现分离
- 使用Hilt进行依赖注入，避免手动创建对象

## 本地缓存功能

### 缓存架构
- **Cache-First策略**: 优先读取本地缓存，缓存为空或过期时请求网络
- **Room数据库**: 使用Room作为本地数据库存储日记数据
- **自动同步**: 通过WorkManager实现后台自动缓存同步
- **缓存过期**: 支持缓存过期机制，默认24小时

### 关键组件
- `DiaryEntity`: 日记数据库实体
- `DiaryDao`: 数据访问对象，提供CRUD操作
- `DiaryLocalRepository`: 本地缓存Repository
- `DiaryRepositoryWithCache`: 带缓存的Repository实现
- `CacheManager`: 缓存管理器，负责后台同步和清理
- `CacheSyncWorker`: 后台缓存同步Worker
- `CacheCleanupWorker`: 缓存清理Worker

### 使用方式

#### Repository层
```kotlin
// 当前使用的DiaryRepository自动支持缓存
class DiaryRepositoryWithCache @Inject constructor(
    private val networkRepository: DiaryRepositoryHttpImpl,
    private val localRepository: DiaryLocalRepository
)
```

#### ViewModel层
```kotlin
// 支持缓存的ViewModel示例
class DiaryHistoryViewModelWithCache @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    // 自动获得缓存支持，无需修改现有代码
    fun loadDiaryList() {
        // 自动先从缓存读取，缓存无效时请求网络
    }
}
```

### 缓存策略详情

#### 读取策略
1. 检查本地缓存是否存在且有效
2. 如果有效，返回缓存数据
3. 如果无效，请求网络数据
4. 网络请求成功后更新缓存
5. 网络请求失败时，尝试返回过期缓存

#### 写入策略
1. 数据写入(保存/删除)优先操作网络
2. 网络操作成功后更新本地缓存
3. 确保数据一致性

#### 后台同步
- 每6小时自动同步缓存
- 需要网络连接和电池电量充足
- 每天自动清理过期缓存

### 开发注意事项
- 缓存功能已集成到现有Repository中，无需修改ViewModel
- 支持离线浏览已缓存的日记
- 缓存失效时会自动重新加载
- 通过CacheManager可手动控制缓存策略