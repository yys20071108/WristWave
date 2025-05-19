package com.wristwave.app.model

import android.net.Uri

/**
 * 表示一个媒体项目（音乐、视频、图片或录音）
 */
data class MediaItem(
    val id: Long,
    val type: String, // "music", "video", "image", "recording"
    val name: String,
    val uri: Uri
)