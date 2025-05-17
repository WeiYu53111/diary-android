package com.wy.diary.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wy.diary.api.RetrofitClient
import com.wy.diary.api.TokenManager
import com.wy.diary.ui.theme.DiaryAndroidTheme

class ImagePreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏显示
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 添加返回键处理
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // 关闭当前活动
            }
        })
        
        // 获取传递的参数
        val imageUrls = intent.getStringArrayListExtra("IMAGE_URLS") ?: ArrayList()
        val initialPosition = intent.getIntExtra("CURRENT_POSITION", 0)
        
        setContent {
            DiaryAndroidTheme {
                ImagePreviewScreen(
                    imageUrls = imageUrls,
                    initialPosition = initialPosition,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewScreen(
    imageUrls: List<String>,
    initialPosition: Int,
    onBackPressed: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPosition,
        pageCount = { imageUrls.size }
    )
    
    var currentPage by remember { mutableStateOf(initialPosition) }
    
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ImagePreviewItem(imageUrl = imageUrls[page])
        }
        
        // 返回按钮 - 修正了修饰符链式调用
        Box(
            modifier = Modifier
                .padding(top = 48.dp, start = 20.dp)  // 增加顶部padding以避免状态栏覆盖
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x88000000))
                .clickable(onClick = onBackPressed)  // 使用这种形式可能更稳定
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }
        
        // 图片计数器
        Box(
            modifier = Modifier
                .padding(top = 48.dp, end = 20.dp)  // 增加顶部padding以避免状态栏覆盖
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color(0x88000000))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentPage + 1} / ${imageUrls.size}",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ImagePreviewItem(imageUrl: String) {
    val context = LocalContext.current
    
    // 处理图片URL
    val processedUrl = remember(imageUrl) {
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            val openid = TokenManager.getToken()
            "${RetrofitClient.BASE_URL}${RetrofitClient.IMAGE_API_PREFX}?id=${openid}&file=${imageUrl}"
        } else {
            imageUrl
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(processedUrl)
                .crossfade(true)
                .build(),
            contentDescription = "图片预览",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun ImagePreviewScreenPreview() {
    val sampleUrls = listOf(
        "https://picsum.photos/800/600",
        "https://picsum.photos/800/601",
        "https://picsum.photos/800/602"
    )
    
    DiaryAndroidTheme {
        ImagePreviewScreen(
            imageUrls = sampleUrls,
            initialPosition = 0,
            onBackPressed = {}
        )
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun ImagePreviewItemPreview() {
    DiaryAndroidTheme {
        ImagePreviewItem(imageUrl = "https://picsum.photos/800/600")
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Image Counter")
fun ImageCounterPreview() {
    DiaryAndroidTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color(0x88000000))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "2 / 5",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Back Button")
fun BackButtonPreview() {
    DiaryAndroidTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }
    }
}