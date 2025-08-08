# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

这是一个基于Android Kotlin的日记应用，使用现代Android开发技术栈：Jetpack Compose、MVVM架构、Hilt依赖注入、Retrofit网络请求等。应用支持写日记、上传图片、备份管理等功能。

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

### MVVM + Repository 模式
- **View层**: Jetpack Compose UI + Activities
- **ViewModel层**: 处理UI状态和业务逻辑，使用 @HiltViewModel
- **Repository层**: 数据访问抽象层，统一本地和远程数据源
- **Data层**: 网络API、数据模型、工具类

### 依赖注入 (Hilt)
- `DiaryApplication`: 使用 @HiltAndroidApp 标注的应用类
- `NetworkModule`: 提供网络相关依赖 (Retrofit, OkHttp, API服务)
- `RepositoryModule`: 绑定Repository接口到具体实现
- `ViewModelBindingModule`: ViewModel依赖注入配置

### 关键目录结构
```
app/src/main/java/com/wy/diary/
├── activity/           # 活动类
├── components/         # 可复用UI组件
├── data/
│   ├── di/            # Hilt依赖注入模块
│   ├── model/         # 数据模型
│   ├── remote/        # 网络API和拦截器
│   └── repository/    # Repository实现
├── ui/
│   ├── screen/        # Compose屏幕
│   └── theme/         # 主题配置
├── util/              # 工具类
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