package com.wy.diary.activity

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wy.diary.R
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.ui.screen.LoginScreen
import com.wy.diary.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {
    
    private val viewModel: LoginViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 如果用户已登录，直接进入主界面
        if (viewModel.checkLoginStatus(this)) {
            navigateToMainActivity()
            return
        }
        
        enableEdgeToEdge()
        setContent {
            DiaryAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    
                    LoginScreen(
                        uiState = uiState,
                        onLoginClick = { viewModel.performWeChatLogin(this) },
                        onTermsClick = { viewModel.openTermsOfService() },
                        onPrivacyClick = { viewModel.openPrivacyPolicy() }
                    )
                    
                    // 监听登录状态，登录成功后跳转
                    LaunchedEffect(uiState.isLoginSuccess) {
                        if (uiState.isLoginSuccess) {
                            navigateToMainActivity()
                        }
                    }
                }
            }
        }
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