package com.flix.videos.ui.app

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.flix.videos.R
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.PlayerActivity
import com.flix.videos.ui.app.player.service.player.PipPlayerService
import com.flix.videos.ui.app.player.service.player.managers.ServiceMediaControllerManager
import com.flix.videos.ui.app.viewmodel.ReadMediaVideosViewModel
import com.flix.videos.ui.app.viewmodel.ViewMode
import org.koin.compose.koinInject

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: ReadMediaVideosViewModel,
    onGroupClick: (String, String) -> Unit
) {
    val videInfos by viewModel.videoInfos.collectAsState()
    val groupedVideos by viewModel.groupedVideos.collectAsState()
    val videosViewMode by viewModel.videosViewMode.collectAsState()

    val context = LocalContext.current

    val serviceMediaControllerManager: ServiceMediaControllerManager =
        koinInject<ServiceMediaControllerManager>()

    val startPlayerActivity: (VideoInfo) -> Unit = { videoInfo ->
        viewModel.makeNewlyAddedMediaIsSeen(videoInfo.id)
        val launchPlayerActivity: () -> Unit = {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .apply {
                        action = "ACTION_NEW_ACTIVITY"
                        data = videoInfo.uri
                        putExtra("video_id", videoInfo.id)
                    })
        }

        if (PipPlayerService
                .isRunning
        ) {
            serviceMediaControllerManager.releaseMediaController()
        }
        launchPlayerActivity()
    }

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
                Text("Videos Flix", style = MaterialTheme.typography.titleMedium)
            }
        }, actions = {
            ViewModeSelector(
                currentMode = videosViewMode,
                onModeChange = {
                    viewModel.saveVideoViewMode(it)
                }
            )
        })

        when (videosViewMode) {
            ViewMode.LIST -> {
                VideosList(
                    videInfos = videInfos,
                    viewModel = viewModel,
                    onItemClick = startPlayerActivity,
                    modifier = Modifier.weight(1f)
                )
            }

            ViewMode.GRID -> {
                VideosGrid(
                    videInfos,
                    modifier = Modifier.weight(1f),
                    onItemClick = startPlayerActivity
                )
            }

            ViewMode.FOLDER -> {
                FoldersScreen(groupedVideos, onGroupClick)
            }
        }
    }
}

@Composable
private fun ViewModeSelector(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Change View"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(8.dp)
        ) {
            ViewMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = currentMode == mode,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(mode.title)
                        }
                    },
                    onClick = {
                        onModeChange(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}