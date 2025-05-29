import org.gradle.internal.impldep.org.eclipse.jgit.transport.ReceiveCommand.link
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
    
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // 添加 Coil 图片加载库
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // 使用阿里云托管的农历库
    implementation("cn.hutool:hutool-core:5.8.15")
    
    // 更新到最新版本（或至少 1.7.0）
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Retrofit 网络请求库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")  // 使用最新版本
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")  // 使用最新版本

    // 新增依赖
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.15.1") // 取消注释并修正格式
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1") // 添加注解处理器
//    implementation("io.coil-kt:coil:2.4.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

        // Accompanist 库
    implementation("com.google.accompanist:accompanist-swiperefresh:0.24.13-rc")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
}