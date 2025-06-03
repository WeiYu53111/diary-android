import org.gradle.internal.impldep.org.eclipse.jgit.transport.ReceiveCommand.link
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") // 应用 Hilt 插件
    kotlin("kapt") // 应用 Kotlin Annotation Processing Tool 插件
    id("kotlin-parcelize")
}

// 读取 local.properties 文件
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

// 提供默认值的辅助函数
fun getLocalProperty(key: String, defaultValue: String): String {
    return localProperties.getProperty(key, defaultValue)
}

android {
    namespace = "com.wy.diary"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.wy.diary"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 添加全局敏感配置
        buildConfigField("String", "DEV_OPEN_ID", "\"${getLocalProperty("dev.open.id", "test_open_id")}\"")
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            // 从local.properties中读取开发环境API地址，默认为模拟器地址
            buildConfigField("String", "API_BASE_URL", "\"${getLocalProperty("api.debug.url", "http://10.0.2.2:7080")}\"")
            buildConfigField("Boolean", "DEBUG_MODE", "true")
            // 可以针对不同环境设置不同的敏感信息
            buildConfigField("String", "ENV_OPEN_ID", "\"${getLocalProperty("dev.open.id", "debug_open_id")}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 从local.properties中读取生产环境API地址
            buildConfigField("String", "API_BASE_URL", "\"${getLocalProperty("api.release.url", "https://api.example.com")}\"")
            buildConfigField("Boolean", "DEBUG_MODE", "false")
            // 生产环境可以使用不同的配置
            buildConfigField("String", "ENV_OPEN_ID", "\"${getLocalProperty("prod.open.id", "")}\"")
        }
        
        // 可选：创建一个测试环境构建类型
//        create("staging") {
//            initWith(getByName("debug"))
//            // 从local.properties中读取测试环境API地址
//            buildConfigField("String", "API_BASE_URL", "\"${getLocalProperty("api.staging.url", "https://staging-api.example.com")}\"")
//            buildConfigField("Boolean", "DEBUG_MODE", "true")
//            isDebuggable = true
//        }
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    
    // 添加 lint 配置块来禁用特定的 lint 检查器
    lint {
        disable += "NullSafeMutableLiveData"
    }
}

dependencies {
    
    // ==== 第三方平台SDK ====
    // 微信开放平台SDK，用于微信登录、分享等功能
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.0")
    
    // ==== Android 官方基础库 ====
    // Android Kotlin 核心扩展库
    implementation(libs.androidx.core.ktx)
    // Android 生命周期运行时库
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Jetpack Compose活动集成库
    implementation(libs.androidx.activity.compose)
    // Compose基础库集合
    implementation(platform(libs.androidx.compose.bom))
    // Compose UI基础组件
    implementation(libs.androidx.ui)
    // Compose图形渲染库
    implementation(libs.androidx.ui.graphics)
    // Compose预览工具
    implementation(libs.androidx.ui.tooling.preview)
    // Material Design 3组件库
    implementation(libs.androidx.material3)
    // 传统视图系统兼容库
    implementation(libs.androidx.appcompat)
    // Material Design组件(非Compose)
    implementation(libs.material)
    // Android Activity库
    implementation(libs.androidx.activity)
    // 约束布局库
    implementation(libs.androidx.constraintlayout)
    // 下拉刷新组件
    implementation(libs.androidx.swiperefreshlayout)
    // LiveData扩展库
    implementation(libs.androidx.lifecycle.livedata.ktx)
    // ViewModel扩展库
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // 导航组件-Fragment支持
    implementation(libs.androidx.navigation.fragment.ktx)
    // 导航组件-UI支持
    implementation(libs.androidx.navigation.ui.ktx)
    
    // ==== 测试相关库 ====
    // 单元测试库
    testImplementation(libs.junit)
    // Android测试扩展库
    androidTestImplementation(libs.androidx.junit)
    // UI自动化测试库
    androidTestImplementation(libs.androidx.espresso.core)
    // Compose测试基础库
    androidTestImplementation(platform(libs.androidx.compose.bom))
    // Compose UI测试
    androidTestImplementation(libs.androidx.ui.test.junit4)
    // Compose工具库(仅调试模式)
    debugImplementation(libs.androidx.ui.tooling)
    // Compose测试清单
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // ==== 图片加载库 ====
    // Coil - Kotlin协程图片加载库(Compose专用)
    implementation("io.coil-kt:coil-compose:2.5.0")
    // Glide - 高效图片加载和缓存库
    implementation("com.github.bumptech.glide:glide:4.15.1")
    // Glide OkHttp集成
    implementation("com.github.bumptech.glide:okhttp3-integration:4.15.1")
    // Glide注解处理器
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    // 圆形图片视图控件
    implementation("de.hdodenhof:circleimageview:3.1.0")
    
    // ==== 日期时间处理 ====
    // Hutool核心库 - 中国农历计算等工具
    implementation("cn.hutool:hutool-core:5.8.15")
    
    // ==== Jetpack Compose扩展 ====
    // 最新版Activity-Compose集成
    implementation("androidx.activity:activity-compose:1.8.2")
    // 最新版Activity-KTX扩展
    implementation("androidx.activity:activity-ktx:1.8.2")
    // ViewModel-Compose集成
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    // Compose Material图标扩展包
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    
    // ==== 网络请求库 ====
    // Retrofit - 类型安全的HTTP客户端
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Retrofit Gson转换器
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp - HTTP客户端基础库
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    // OkHttp日志拦截器 - 用于调试网络请求
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // ==== 协程相关 ====
    // Kotlin协程Android支持库
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // ==== Google Accompanist库 ====
    // 下拉刷新组件
    implementation("com.google.accompanist:accompanist-swiperefresh:0.24.13-rc")
    
    // ==== 数据存储 ====
    // Jetpack DataStore - 键值对存储(替代SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Hilt 核心依赖
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1") // kapt 用于注解处理器

    // 确保 work-runtime-ktx 是 2.7.0 或更高版本
    implementation("androidx.work:work-runtime-ktx:2.9.0") // 或者更新到最新稳定版
    // 如果您使用 ViewModel，需要添加 Hilt 的 ViewModel 依赖
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // 如果是Compose
    implementation("androidx.hilt:hilt-work:1.2.0") // 如果是WorkManager
    kapt("androidx.hilt:hilt-compiler:1.2.0") // 对应 Hilt 的 ViewModel/WorkManager 编译器

}