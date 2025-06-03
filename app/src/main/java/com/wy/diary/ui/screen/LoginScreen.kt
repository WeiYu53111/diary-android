package com.wy.diary.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.wy.diary.R
import com.wy.diary.data.model.LoginUiState
import com.wy.diary.ui.theme.DiaryAndroidTheme

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLoginClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val context = LocalContext.current
    
    // 显示错误信息
    if (uiState.error != null) {
        Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
    }
    
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
                onClick = onLoginClick,
                enabled = !uiState.isLoading,
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
                    // 微信图标
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
                    
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = if (uiState.isLoading) "登录中..." else "微信一键登录",
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
                    modifier = Modifier.clickable(onClick = onTermsClick)
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
                    modifier = Modifier.clickable(onClick = onPrivacyClick)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    DiaryAndroidTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onLoginClick = {},
            onTermsClick = {},
            onPrivacyClick = {}
        )
    }
}