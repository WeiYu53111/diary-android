package com.wy.diary.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wy.diary.R
import com.wy.diary.api.RetrofitClient
import com.wy.diary.ui.theme.DiaryAndroidTheme
import com.wy.diary.viewmodel.DiaryItem

/**
 * 日记卡片组件
 *
 * @param diary 日记数据项
 * @param onDiaryClick 点击日记的回调
 * @param onDeleteClick 点击删除的回调
 * @param onImageClick 点击图片的回调，传入图片URL列表和点击的索引
 * @param modifier Modifier修饰符
 */
@Composable
fun DiaryCard(
    diary: DiaryItem,
    onDiaryClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDiaryClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 日记顶部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日期信息
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = diary.logTime,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = diary.logWeek,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    
                    // 农历显示
                    diary.logLunar?.let {
                        if (it.isNotEmpty()) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // 删除按钮
                TextButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.Gray
                    )
                    Text("删除", color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 日记内容预览
            Text(
                text = diary.contentPreview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // 图片预览
            if (diary.images.isNotEmpty()) {
                ImagePreviewGrid(
                    images = diary.images,
                    onImageClick = onImageClick // 直接传递回调
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 底部信息：时间和地点
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 创建时间
                Text(
                    text = diary.createTimeFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                // 位置信息
                diary.address?.let {
                    if (it.isNotEmpty() && it != "未选择地址") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "位置",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 图片预览网格
 *
 * @param images 图片URL列表
 * @param onImageClick 图片点击回调
 * @param modifier Modifier修饰符
 */
@Composable
fun ImagePreviewGrid(
    images: List<String>,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images) { imageUrl ->
            //val finalImageUrl = processImageUrl(imageUrl)
            val index = images.indexOf(imageUrl)
            
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "日记图片",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick(images, index) },
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.image_error),
                error = painterResource(id = R.drawable.image_error)
            )
        }
    }
}

/**
 * 空日记视图
 *
 * @param onCreateClick 创建按钮点击回调
 * @param modifier Modifier修饰符
 */
@Composable
fun EmptyDiaryView(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.empty_diary),
            contentDescription = "无日记",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "还没有创建日记~",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onCreateClick) {
            Text("写日记")
        }
    }
}



// 预览
@Preview(name = "日记卡片", showBackground = true)
@Composable
fun DiaryCardPreview() {
    DiaryAndroidTheme {
        DiaryCard(
            diary = DiaryItem(
                diaryId = "123456",
                logTime = "2025年5月15日",
                logWeek = "星期四",
                logLunar = "四月初八",
                contentPreview = "今天是个美好的一天，阳光明媚，我和朋友一起去公园散步，感觉非常放松...",
                content = "今天是个美好的一天，阳光明媚，我和朋友一起去公园散步，感觉非常放松...",
                images = listOf("image1.jpg", "image2.jpg"),
                createTimeFormatted = "2025-05-15 14:30",
                address = "上海市浦东新区"
            ),
            onDiaryClick = { },
            onDeleteClick = { },
            onImageClick = { _, _ -> },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "图片网格", showBackground = true)
@Composable
fun ImagePreviewGridPreview() {
    DiaryAndroidTheme {
        ImagePreviewGrid(
            images = listOf("default_avatar.jpg", "default_avatar.jpg", "default_avatar.jpg"),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "空日记视图", showBackground = true)
@Composable
fun EmptyDiaryViewPreview() {
    DiaryAndroidTheme {
        EmptyDiaryView(
            onCreateClick = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}