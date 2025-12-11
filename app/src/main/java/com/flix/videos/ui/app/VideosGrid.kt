package com.flix.videos.ui.app

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.ACTION_BROADCAST_CONTROL
import com.flix.videos.ui.app.player.EXTRA_CONTROL_CLOSE
import com.flix.videos.ui.app.player.EXTRA_CONTROL_TYPE
import com.flix.videos.ui.app.player.PlayerActivity
import com.flix.videos.ui.utils.NoIndicationInteractionSource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosGrid(
    videInfos: List<VideoInfo>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyVerticalStaggeredGrid(
        modifier = modifier.fillMaxSize(),
        columns = StaggeredGridCells.Adaptive(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(
            videInfos,
            key = { _, item -> item.uri }) { _, videoInfo ->
            videoInfo.thumbnail?.let {
                OutlinedCard (
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = remember { NoIndicationInteractionSource() },
                    onClick = {
                        val intent = Intent(ACTION_BROADCAST_CONTROL).apply {
                            `package` = context.packageName
                            putExtra(EXTRA_CONTROL_TYPE, EXTRA_CONTROL_CLOSE)
                        }
                        context.sendBroadcast(intent)
                        context.startActivity(
                            Intent(context, PlayerActivity::class.java)
                                .apply {
                                    data = videoInfo.uri
                                    putExtra("video_id", videoInfo.id)
                                    putExtra("title", videoInfo.title)
                                    putExtra("video_width", videoInfo.width)
                                    putExtra("video_height", videoInfo.height)
                                    putExtra("total_duration", videoInfo.duration)
                                })
                    }
                ) {
                    Image(
                        it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
