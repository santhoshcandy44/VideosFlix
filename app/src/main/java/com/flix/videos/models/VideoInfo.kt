package com.flix.videos.models

import android.graphics.Bitmap
import android.net.Uri

data class VideoInfo(
    val uri: Uri,
    val path: String,
    val displayName: String,
    val title:String,
    val width: Int,
    val height: Int,
    val duration: Long,
    val size: Long,
    val mimeType: String,
    val dateAdded: Long,
    val thumbnail: Bitmap?,
    val displayGroupName:String = ""
)
