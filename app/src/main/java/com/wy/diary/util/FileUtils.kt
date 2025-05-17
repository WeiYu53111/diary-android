package com.wy.diary.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun uriToFile(context: Context, uri: Uri): File {
        val contentResolver: ContentResolver = context.contentResolver
        
        // 创建临时文件
        val fileName = "temp_image_${System.currentTimeMillis()}"
        val fileExtension = getFileExtension(contentResolver, uri)
        val tempFile = File.createTempFile(fileName, fileExtension, context.cacheDir)
        
        // 将URI内容复制到临时文件
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        
        return tempFile
    }
    
    fun getFileExtension(contentResolver: ContentResolver, uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return if (mimeType == null) {
            ".jpg" // 默认扩展名
        } else {
            when (mimeType) {
                "image/jpeg" -> ".jpg"
                "image/png" -> ".png"
                "image/gif" -> ".gif"
                else -> "." + mimeType.substring(mimeType.lastIndexOf("/") + 1)
            }
        }
    }


    /**
     * 从 Uri 获取文件路径，支持不同类型的 URI
     */
    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        return try {
            when {
                DocumentsContract.isDocumentUri(context, uri) -> {
                    // 处理文档类型URI
                    when {
                        isExternalStorageDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            val type = split[0]
                            if ("primary".equals(type, ignoreCase = true)) {
                                return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                            } else {
                                // 如果不是主外部存储，尝试获取显示名称
                                return getDisplayNameFromUri(context, uri)
                            }
                        }
                        isDownloadsDocument(uri) -> {
                            try {
                                val id = DocumentsContract.getDocumentId(uri)
                                if (id.startsWith("raw:")) {
                                    // 处理 raw:/ 类型
                                    return id.substring(4)
                                }
                                val contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"), id.toLong()
                                )
                                return getDataColumn(context, contentUri, null, null)
                                    ?: getDisplayNameFromUri(context, uri)
                            } catch (e: Exception) {
                                // 如果以上方法失败，返回显示名称
                                return getDisplayNameFromUri(context, uri)
                            }
                        }
                        isMediaDocument(uri) -> {
                            // 处理媒体文档
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            val type = split[0]
                            var contentUri: Uri? = null
                            when (type) {
                                "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            }
                            return contentUri?.let {
                                getDataColumn(context, it, "_id=?", arrayOf(split[1]))
                            } ?: getDisplayNameFromUri(context, uri)
                        }
                        else -> {
                            // 其他文档类型，尝试获取显示名称
                            return getDisplayNameFromUri(context, uri)
                        }
                    }
                }
                "content".equals(uri.scheme, ignoreCase = true) -> {
                    // 处理内容URI
                    return getDataColumn(context, uri, null, null) ?: getDisplayNameFromUri(context, uri)
                }
                "file".equals(uri.scheme, ignoreCase = true) -> {
                    // 处理文件URI
                    return uri.path
                }
                else -> {
                    // 其他类型URI，尝试获取显示名称
                    return getDisplayNameFromUri(context, uri)
                }
            }
        } catch (e: Exception) {
            Log.e("DataBackupViewModel", "获取文件路径失败", e)
            return getDisplayNameFromUri(context, uri)
        }
    }

    /**
     * 从URI获取显示名称，适用于无法获取实际路径的情况
     */
    fun getDisplayNameFromUri(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DataBackupViewModel", "获取显示名称失败", e)
        }
        
        // 如果无法获取显示名称，使用URI字符串的最后部分
        val decodedUri = Uri.decode(uri.toString())
        val lastPathSegment = uri.lastPathSegment
        
        return lastPathSegment
            ?: if (decodedUri.contains("/")) decodedUri.substringAfterLast("/") 
            else "backup_file_${System.currentTimeMillis()}"
    }


        /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024f)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024f * 1024f))
            else -> String.format("%.1f GB", size / (1024f * 1024f * 1024f))
        }
    }

    /**
     * 判断是否是外部存储文档
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * 判断是否是下载文档
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * 判断是否是媒体文档
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

/**
     * 从URI获取文件大小
     */
    fun getFileSizeFromUri(context: Context, uri: Uri): String {
        try {
            // 首先尝试使用文件描述符获取大小
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fileSize = pfd.statSize
                return formatFileSize(fileSize)
            }
            
            // 如果上一步失败，尝试查询内容提供者
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !it.isNull(sizeIndex)) {
                        val size = it.getLong(sizeIndex)
                        return formatFileSize(size)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DataBackupViewModel", "获取文件大小失败: ${e.message}", e)
        }
        
        return "未知大小"
    }


    /**
     * 获取数据列
     */
    fun getDataColumn(
        context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = MediaStore.MediaColumns.DATA
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            Log.e("DataBackupViewModel", "获取数据列失败: ${e.message}", e)
        } finally {
            cursor?.close()
        }
        return null
    }

}