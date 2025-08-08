package com.wy.diary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 深色模式颜色
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CCDDC),       // 柔和蓝色
    onPrimary = Color(0xFF003544),     // 深蓝色
    primaryContainer = Color(0xFF004D61), // 中蓝色
    onPrimaryContainer = Color(0xFFCBE6F1), // 浅蓝色
    secondary = Color(0xFFB3CBD5),     // 灰蓝色
    onSecondary = Color(0xFF1F333B),   // 深灰蓝
    background = Color(0xFF191C1E),    // 几乎黑色
    onBackground = Color(0xFFE1E3E4),  // 浅灰色
    surface = Color(0xFF191C1E),       // 几乎黑色
    onSurface = Color(0xFFE1E3E4)      // 浅灰色
)

// 浅色模式颜色
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006782),       // 中蓝色
    onPrimary = Color(0xFFFFFFFF),     // 白色
    primaryContainer = Color(0xFFBDE9FF), // 浅蓝色
    onPrimaryContainer = Color(0xFF001F2A), // 深蓝色
    secondary = Color(0xFF4B6269),     // 灰蓝色
    onSecondary = Color(0xFFFFFFFF),   // 白色
    background = Color(0xFFFAFDFD),    // 几乎白色
    onBackground = Color(0xFF191C1E),  // 深色
    surface = Color(0xFFFAFDFD),       // 几乎白色
    onSurface = Color(0xFF191C1E)      // 深色
)

@Composable
fun DiaryAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}