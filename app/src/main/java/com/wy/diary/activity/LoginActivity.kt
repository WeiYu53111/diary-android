package com.wy.diary.activity

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wy.diary.BuildConfig
import com.wy.diary.R
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.api.TokenManager

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 未登录，显示登录界面
        enableEdgeToEdge()
        setContent {
            DiaryAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            navigateToMainActivity()
                        }
                    )
                }
            }
        }

        // 模拟登录延迟
        android.os.Handler().postDelayed({
            // 模拟微信登录返回的 open_id
            val mockOpenId = BuildConfig.DEV_OPEN_ID
            // 使用 TokenManager 保存令牌
            TokenManager.saveToken(mockOpenId)
            navigateToMainActivity()

        }, 500)

    }

    /**
     * 检查用户是否已登录
     */
    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }
    
    /**
     * 导航到主页面
     */
    private fun navigateToMainActivity() {
        // 创建 Intent
        val intent = Intent(this, MainActivity::class.java)

        // 使用 ActivityOptions 创建转场动画
        val options = ActivityOptions.makeCustomAnimation(
            this,
            R.anim.slide_in_right,  // 新活动进入的动画
            R.anim.slide_out_left   // 当前活动退出的动画
        )

        // 使用转场动画启动活动
        startActivity(intent, options.toBundle())

        // 结束当前页面，防止用户按返回键返回登录页
        finish()
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 应用Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "日记应用Logo",
                    modifier = Modifier.size(120.dp),
                    // 保持图像在圆形内正确显示
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 欢迎文字
            Text(
                text = "欢迎使用fish日记",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "记录每一天的心情与收获",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 微信登录按钮
            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        // 这里实际项目中需要接入微信SDK进行登录
                        // 模拟登录过程
                        Toast.makeText(context, "正在调用微信登录...", Toast.LENGTH_SHORT).show()

                        // 模拟微信登录返回的 open_id
                        val mockOpenId = BuildConfig.DEV_OPEN_ID

                        // 使用 TokenManager 保存令牌
                        TokenManager.saveToken(mockOpenId)

                        // 同时更新登录状态
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("is_logged_in", true)
                            .apply()

                        isLoading = false
                        onLoginSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF07C160) // 微信绿色
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 微信图标（实际项目需要添加微信图标资源）
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "W",
                            color = Color(0xFF07C160),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = if (isLoading) "登录中..." else "微信一键登录",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 使用条款和隐私政策
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "登录即表示您同意",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "用户协议",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { /* 打开用户协议 */ }
                )
                Text(
                    text = "和",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "隐私政策",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { /* 打开隐私政策 */ }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    DiaryAndroidTheme {
        LoginScreen(onLoginSuccess = {})
    }
}