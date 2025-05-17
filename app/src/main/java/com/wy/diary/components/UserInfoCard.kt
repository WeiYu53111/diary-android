package com.wy.diary.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wy.diary.R
import com.wy.diary.ui.theme.DiaryAndroidTheme

/**
 * 用户信息卡片组件
 * 
 * @param diaryCount 日记数量
 * @param registerTime 注册时间
 * @param userName 用户名（可选）
 * @param avatarResId 头像资源ID（可选）
 * @param onBackupClick 点击备份按钮的回调
 * @param modifier Modifier修饰符
 */
@Composable
fun UserInfoCard(
    diaryCount: Int,
    registerTime: String,
    userName: String = "用户名",
    avatarResId: Int = R.drawable.default_avatar,
    onBackupClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "头像",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$diaryCount 篇日记",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = registerTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // 数据备份按钮
            IconButton(onClick = onBackupClick) {
                Icon(
                    painter = painterResource(id = R.drawable.download_24px), // 加载自定义矢量图
                    // 或直接使用Material Icons：
                    // imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black // 设置颜色
                )
            }
        }
    }
}

@Preview(name = "用户信息卡片", showBackground = true)
@Composable
fun UserInfoCardPreview() {
    DiaryAndroidTheme {
        UserInfoCard(
            diaryCount = 42,
            registerTime = "注册于 2024-12-25",
            modifier = Modifier.padding(16.dp)
        )
    }
}