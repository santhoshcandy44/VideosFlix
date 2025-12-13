package com.flix.videos.ui.app

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flix.videos.ui.app.player.ACTION_BROADCAST_CONTROL
import com.flix.videos.ui.app.player.EXTRA_CONTROL_CLOSE
import com.flix.videos.ui.app.player.EXTRA_CONTROL_TYPE
import com.flix.videos.ui.app.player.PlayerActivity
import com.flix.videos.ui.app.viewmodel.ReadMediaVideosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupVideosScreen(
    group: String,
    groupName: String,
    viewModel: ReadMediaVideosViewModel,
    onPopUp: () -> Unit
) {
    val groupedVideos by viewModel.groupedVideos.collectAsState()
    val videInfos = groupedVideos[group] ?: emptyList()

    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = {
                Text("Back", style = MaterialTheme.typography.titleMedium)
            },
            navigationIcon = {
                IconButton(onClick = onPopUp) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                }
            }
        )
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Folder, null)
            Text(groupName, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(8.dp))
        VideosList(
            videInfos = videInfos,
            viewModel = viewModel,
            onItemClick = { videoInfo ->
                val intent = Intent(ACTION_BROADCAST_CONTROL).apply {
                    `package` = context.packageName
                    putExtra(EXTRA_CONTROL_TYPE, EXTRA_CONTROL_CLOSE)
                }
                context.sendBroadcast(intent)
                context.startActivity(
                    Intent(context, PlayerActivity::class.java)
                        .apply {
                            data = videoInfo.uri
                            putExtra("group", group)
                            putExtra("video_id", videoInfo.id)
                            putExtra("title", videoInfo.title)
                            putExtra("video_width", videoInfo.width)
                            putExtra("video_height", videoInfo.height)
                            putExtra("total_duration", videoInfo.duration)
                        })
            },
            modifier = Modifier.weight(1f)
        )
    }
}