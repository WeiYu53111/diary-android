package com.wy.diary.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wy.diary.ui.theme.DiaryAndroidTheme

/**
 * 表示标签项数据
 */
data class TabItemData(
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int = 0
)

/**
 * 应用底部导航栏，可在多个Activity中复用
 */
@Composable
fun CustomTabBar(
    activeIndex: Int,
    onTabSelected: (Int) -> Unit,
    onWriteClick: () -> Unit = {}, // 写日记回调
    onHistoryClick: () -> Unit = {}, // 历史日记回调
    items: List<TabItemData> = listOf(
        TabItemData(Icons.Default.Edit, "写日记"),
        TabItemData(Icons.Default.Person, "我的日记") 
    )
) {
    // 适配不同屏幕尺寸
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),  // 增加高度以改善可访问性
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = activeIndex == index
                val onClick = {
                    onTabSelected(index)
                    when (index) {
                        0 -> onWriteClick()
                        1 -> onHistoryClick()
                    }
                }
                
                TabItem(
                    icon = item.icon,
                    label = item.label,
                    isSelected = isSelected,
                    badgeCount = item.badgeCount,
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用动画平滑过渡颜色变化
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                      else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary 
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "textColor"
    )
    
    // 使用动画平滑过渡内边距变化
    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 12.dp,
        animationSpec = tween(300),
        label = "padding"
    )

    // 创建语义化的无障碍描述
    val stateDescription = if (isSelected) "已选择" else "未选择"
    val tabDescription = "$label, $stateDescription"
    
    // 添加涟漪效果
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .semantics {
                contentDescription = tabDescription
                role = Role.Tab
            }
            .combinedClickable(
                onClick = onClick,
                indication = rememberRipple(bounded = true),
                interactionSource = interactionSource
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )

                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = label,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Preview(name = "Custom Tab Bar", showBackground = true)
@Composable
fun CustomTabBarPreview() {
    DiaryAndroidTheme {
        CustomTabBar(
            activeIndex = 0,
            onTabSelected = {},
            onWriteClick = {},
            onHistoryClick = {}
        )
    }
}

@Preview(name = "Custom Tab Bar With Badge", showBackground = true)
@Composable
fun CustomTabBarWithBadgePreview() {
    DiaryAndroidTheme {
        CustomTabBar(
            activeIndex = 0,
            onTabSelected = {},
            onWriteClick = {},
            onHistoryClick = {},
            items = listOf(
                TabItemData(Icons.Default.Edit, "写日记"),
                TabItemData(Icons.Default.Person, "我的日记", 5)
            )
        )
    }
}

@Preview(name = "Tab Items", showBackground = true)
@Composable
fun TabItemsPreview() {
    DiaryAndroidTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem(
                icon = Icons.Default.Edit,
                label = "写日记",
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            
            TabItem(
                icon = Icons.Default.Person,
                label = "我的日记",
                isSelected = true,
                badgeCount = 3,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}