package com.wy.diary.ui.screen

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wy.diary.data.model.ImagePreviewUiState
import com.wy.diary.viewmodel.ImagePreviewViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewScreen(
    viewModel: ImagePreviewViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ImagePreviewContent(
        uiState = uiState,
        onPageChange = { viewModel.updateCurrentPosition(it) },
        onBackPressed = onBackPressed
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewContent(
    uiState: ImagePreviewUiState,
    onPageChange: (Int) -> Unit,
    onBackPressed: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPosition,
        pageCount = { uiState.imageUrls.size }
    )
    
    // 监听页面变化
    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
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
            ImagePreviewItem(
                imageUrl = uiState.imageUrls[page]
            )
        }
        
        // 返回按钮
        BackButton(onBackPressed = onBackPressed)
        
        // 图片计数器
        ImageCounter(
            current = uiState.currentPosition + 1,
            total = uiState.imageUrls.size
        )
    }
}

@Composable
fun ImagePreviewItem(
    imageUrl: String
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "图片预览",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BackButton(onBackPressed: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = 48.dp, start = 20.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x88000000))
            .clickable(onClick = onBackPressed),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "返回",
            tint = Color.White
        )
    }
}

@Composable
fun ImageCounter(current: Int, total: Int) {
    Box(
        modifier = Modifier
            .padding(top = 48.dp, end = 20.dp)
            //.align(Alignment.TopEnd)
            .clip(CircleShape)
            .background(Color(0x88000000))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$current / $total",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}