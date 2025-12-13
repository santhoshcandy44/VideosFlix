package com.flix.videos.ui.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.flix.videos.R
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.viewmodel.ReadMediaVideosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: ReadMediaVideosViewModel,
    onGroupClick: (String, String) -> Unit
) {
    val groupedVideos by viewModel.groupedVideos.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.ic_nav), contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text("Videos", style = MaterialTheme.typography.titleMedium)
            }
        })

        LazyVerticalStaggeredGrid(
            modifier = Modifier.weight(1f),
            columns = StaggeredGridCells.Adaptive(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(
                groupedVideos.toList(),
                key = { _, item -> item.first }) { index, (group, value) ->
                Card(
                    modifier = Modifier,
                    onClick = {
                        onGroupClick(group, value.first().displayGroupName)
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(value.first().displayGroupName)

                        FourColumnGrid(
                            value.take(4), endContent = if (value.size > 4) {
                                {
                                    CountBadge(
                                        "${value.size - 4}+",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                                        style = LocalTextStyle.current.copy(
                                            fontSize = 24.sp,
                                            lineHeight = 24.sp,
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            )
                                        )
                                    )
                                }
                            } else null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FourColumnGrid(
    videoList: List<VideoInfo>,
    endContent: (@Composable () -> Unit)? = null
) {
    BoxWithConstraints {
        val itemSpacing = 6.dp
        val totalSpacing = itemSpacing * 1
        val itemSize = (maxWidth - totalSpacing) / 2

        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            videoList.forEachIndexed { index, videoInfo ->
                Box(
                    modifier = Modifier
                        .width(itemSize)
                        .aspectRatio(1f)
                        .aspectRatio(1f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(videoInfo.thumbnail)
                            .diskCacheKey(videoInfo.uri.toString())
                            .memoryCacheKey(videoInfo.uri.toString())
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (index == videoList.size - 1) {
                        endContent?.let {
                            it()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = style
        )
    }
}