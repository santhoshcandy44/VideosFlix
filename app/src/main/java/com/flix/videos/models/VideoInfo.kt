package com.flix.videos.models

import android.graphics.Bitmap
import android.net.Uri
import com.flix.videos.ui.app.player.viewmodel.AudioTrackInfo
import com.flix.videos.ui.app.player.viewmodel.SubtitleTrackInfo

data class VideoInfo(
    val id:Long,
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
    val displayGroupName:String = "",
    val audioTrackInfos: List<AudioTrackInfo> = emptyList(),
    val subtitleTrackInfos: List<SubtitleTrackInfo> = emptyList()
){
    companion object {
        val EMPTY  = VideoInfo(
            id = 0L,
            uri = Uri.EMPTY,
            path = "",
            displayName = "",
            title = "",
            width = 0,
            height = 0,
            duration = 0L,
            size = 0L,
            mimeType = "",
            dateAdded = 0L,
            thumbnail = null,
            displayGroupName = "",
            audioTrackInfos = emptyList(),
            subtitleTrackInfos = emptyList()
        )
    }
}
