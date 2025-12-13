package com.flix.videos.ui.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flix.videos.R
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.utils.noRippleClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    groupedVideos: Map<String, List<VideoInfo>>,
    onGroupClick: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            groupedVideos.toList(),
            key = { _, item -> item.first }) { index, (group, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable{
                        onGroupClick(group, value.first().displayGroupName)
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(modifier = Modifier.wrapContentSize()) {
                    Image(
                        painterResource(R.drawable.folder),
                        contentDescription = "Folder",
                        modifier = Modifier.size(56.dp)
                    )
                    FolderCountBadge(
                        value.size.toString(),
                        modifier =
                            Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red.copy(alpha = 0.9f), CircleShape)
                                .clip(CircleShape),
                        style = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }

                Text(
                    value.first().displayGroupName,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FolderCountBadge(
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