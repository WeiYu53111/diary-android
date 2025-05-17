package com.wy.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wy.diary.ui.theme.DiaryAndroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 高级日记编辑器组件
 *
 * @param text 编辑器文本内容
 * @param onTextChange 文本变更回调
 * @param modifier Modifier修饰符
 * @param isBold 是否粗体
 * @param isItalic 是否斜体
 * @param isUnderlined 是否下划线
 * @param onBoldChange 粗体状态变更回调
 * @param onItalicChange 斜体状态变更回调
 * @param onUnderlinedChange 下划线状态变更回调
 */
@Composable
fun DiaryEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isUnderlined: Boolean = false,
    onBoldChange: (Boolean) -> Unit = {},
    onItalicChange: (Boolean) -> Unit = {},
    onUnderlinedChange: (Boolean) -> Unit = {}
) {
    // 将文本包装为 TextFieldValue 以便更好地控制光标位置
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text))
    }
    
    // 在文本变化时同步回调
    val onValueChange: (TextFieldValue) -> Unit = { newValue ->
        textFieldValue = newValue
        onTextChange(newValue.text)
    }
    
    // 获取行号信息
    val lineCount = text.lines().size
    
    // 计算当前行号
    val currentLine = text.substring(0, textFieldValue.selection.max).count { it == '\n' } + 1
    
    Column(modifier = modifier) {
        // 格式工具栏
        FormatToolbar(
            isBold = isBold,
            isItalic = isItalic, 
            isUnderlined = isUnderlined,
            onBoldChange = onBoldChange,
            onItalicChange = onItalicChange,
            onUnderlinedChange = onUnderlinedChange,
            onInsertDate = {
                // 在当前光标位置插入日期
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val beforeCursor = textFieldValue.text.substring(0, textFieldValue.selection.start)
                val afterCursor = textFieldValue.text.substring(textFieldValue.selection.end)
                val newText = "$beforeCursor$currentDate$afterCursor"
                val newCursorPosition = beforeCursor.length + currentDate.length
                
                textFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursorPosition)
                )
                onTextChange(newText)
            },
            onInsertEmoji = {
                // 插入表情（简单演示）
                val emoji = "😊"
                val beforeCursor = textFieldValue.text.substring(0, textFieldValue.selection.start)
                val afterCursor = textFieldValue.text.substring(textFieldValue.selection.end)
                val newText = "$beforeCursor$emoji$afterCursor"
                val newCursorPosition = beforeCursor.length + emoji.length
                
                textFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursorPosition)
                )
                onTextChange(newText)
            }
        )
        
        // 状态栏
        EditorStatusBar(
            currentLine = currentLine,
            totalLines = lineCount,
            charCount = text.length
        )
        
        // 编辑器主体
        Row(modifier = Modifier.fillMaxSize()) {
            // 行号区域
            LineNumbers(
                lineCount = lineCount,
                currentLine = currentLine
            )
            
            // 文本编辑区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // 使用SelectionContainer包装，支持文本选择
                SelectionContainer {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )
                }
            }
        }
    }
}

/**
 * 编辑器格式工具栏
 */
@Composable
private fun FormatToolbar(
    isBold: Boolean,
    isItalic: Boolean,
    isUnderlined: Boolean,
    onBoldChange: (Boolean) -> Unit,
    onItalicChange: (Boolean) -> Unit,
    onUnderlinedChange: (Boolean) -> Unit,
    onInsertDate: () -> Unit,
    onInsertEmoji: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 粗体按钮
            IconButton(onClick = { onBoldChange(!isBold) }) {
                Icon(
                    imageVector = Icons.Default.FormatBold,
                    contentDescription = "粗体",
                    tint = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 斜体按钮
            IconButton(onClick = { onItalicChange(!isItalic) }) {
                Icon(
                    imageVector = Icons.Default.FormatItalic,
                    contentDescription = "斜体",
                    tint = if (isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 下划线按钮
            IconButton(onClick = { onUnderlinedChange(!isUnderlined) }) {
                Icon(
                    imageVector = Icons.Outlined.FormatUnderlined,  // 使用 Outlined 变体
                    contentDescription = "下划线",
                    tint = if (isUnderlined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 插入日期按钮
            IconButton(onClick = onInsertDate) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "插入日期",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 插入表情按钮
            IconButton(onClick = onInsertEmoji) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "插入表情",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 行号显示组件
 */
@Composable
private fun LineNumbers(
    lineCount: Int,
    currentLine: Int
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            for (i in 1..lineCount) {
                Text(
                    text = "$i",
                    fontSize = 12.sp,
                    color = if (i == currentLine) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 编辑器状态栏
 */
@Composable
private fun EditorStatusBar(
    currentLine: Int,
    totalLines: Int,
    charCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "行 $currentLine/$totalLines",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "$charCount 字符",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiaryEditorPreview() {
    var previewText by remember { mutableStateOf("这是一个日记编辑器示例。\n可以添加多行内容。\n支持行号显示。") }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderlined by remember { mutableStateOf(false) }
    
    DiaryAndroidTheme {
        DiaryEditor(
            text = previewText,
            onTextChange = { previewText = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .border(1.dp, Color.Gray),
            isBold = isBold,
            isItalic = isItalic,
            isUnderlined = isUnderlined,
            onBoldChange = { isBold = it },
            onItalicChange = { isItalic = it },
            onUnderlinedChange = { isUnderlined = it }
        )
    }
}