package com.flix.videos.ui.app

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flix.videos.R
import com.flix.videos.viewmodel.ReadMediaVideosViewModel
import com.flix.videos.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(viewModel: ReadMediaVideosViewModel) {
    val videInfos by viewModel.videoInfos.collectAsState()
    val videosViewMode by viewModel.videosViewMode.collectAsState()

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
        }, actions = {
            ViewModeSelector(
                currentMode = videosViewMode,
                onModeChange = {
                    viewModel.saveVideoViewMode(it)
                }
            )
        })

        PermissionWrapper(onPermissionGranted = {
            viewModel.fetchVideoInfos()
        }) {
            if (videosViewMode == ViewMode.GRID) {
                VideosGrid(
                    videInfos,
                    modifier = Modifier.weight(1f)
                )
            } else {
                VideosList(
                    videInfos,
                    viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ViewModeSelector(
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
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = currentMode == ViewMode.LIST,
                            onClick = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("List View")
                    }
                },
                onClick = {
                    onModeChange(ViewMode.LIST)
                    expanded = false
                },
            )

            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = currentMode == ViewMode.GRID,
                            onClick = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Grid View")
                    }
                },
                onClick = {
                    onModeChange(ViewMode.GRID)
                    expanded = false
                }
            )
        }
    }
}
