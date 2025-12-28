package com.flix.videos.ui.app

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
import com.flix.videos.ui.utils.NoIndicationInteractionSource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosGrid(
    videInfos: List<VideoInfo>,
    modifier: Modifier = Modifier,
    onItemClick: (VideoInfo) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        contentPadding = PaddingValues(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(
            videInfos,
            key = { _, item -> item.uri }) { _, videoInfo ->
            videoInfo.thumbnail?.let {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = remember { NoIndicationInteractionSource() },
                    onClick = {
                        onItemClick(videoInfo)
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
