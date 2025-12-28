package com.flix.videos.ui.app

import android.app.ActivityOptions
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.flix.videos.R
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.bottombar.GroupVideos
import com.flix.videos.ui.app.bottombar.NavigationBarItemInfo
import com.flix.videos.ui.app.bottombar.NavigationBarRoutes
import com.flix.videos.ui.app.player.PlayerActivity
import com.flix.videos.ui.app.player.service.player.PipPlayerService
import com.flix.videos.ui.app.player.service.player.managers.ServiceMediaControllerManager
import com.flix.videos.ui.app.viewmodel.ReadMediaVideosViewModel
import com.flix.videos.ui.utils.NoIndicationInteractionSource
import com.flix.videos.ui.utils.SwingEdgeToEdgeEffect
import org.koin.compose.koinInject

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVideosScreen(viewModel: ReadMediaVideosViewModel) {
    val context = LocalContext.current
    val videosBackStack = viewModel.videosBackstack
    val albumsBackStack = viewModel.albumsBackStack

    val serviceMediaControllerManager: ServiceMediaControllerManager =
        koinInject<ServiceMediaControllerManager>()

    val startPlayerActivity: (String, VideoInfo) -> Unit = { group, videoInfo ->
        viewModel.makeNewlyAddedMediaIsSeen(videoInfo.id)
        val launchPlayerActivity: () -> Unit = {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .apply {
                        action = "ACTION_NEW_ACTIVITY"
                        data = videoInfo.uri
                        putExtra("video_id", videoInfo.id)
                        putExtra("group", group)
                    },
                ActivityOptions.makeCustomAnimation(
                    context,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                ).toBundle()
            )
        }

        if (PipPlayerService.isRunning)
            serviceMediaControllerManager.releaseMediaController()
        launchPlayerActivity()
    }

    val videosNavEntries = rememberDecoratedNavEntries(
        backStack = videosBackStack,
        entryProvider = entryProvider {
            entry<NavigationBarRoutes.Videos> {
                VideosScreen(viewModel, onGroupClick = { group, groupName ->
                    videosBackStack.add(GroupVideos(group, groupName))
                })
            }

            entry<GroupVideos> {
                GroupVideosScreen(it.group, it.groupName, viewModel, { videoInfo ->
                    startPlayerActivity(it.group, videoInfo)
                }) {
                    videosBackStack.removeLastOrNull()
                }
            }
        },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )
    )

    val albumsNavEntries = rememberDecoratedNavEntries(
        backStack = albumsBackStack,
        entryProvider = entryProvider {
            entry<NavigationBarRoutes.Albums> {
                AlbumsScreen(viewModel) { group, groupName ->
                    albumsBackStack.add(GroupVideos(group, groupName))
                }
            }

            entry<GroupVideos> {
                GroupVideosScreen(it.group, it.groupName, viewModel, { videoInfo ->
                    startPlayerActivity(it.group, videoInfo)
                }) {
                    albumsBackStack.removeLastOrNull()
                }
            }
        },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )
    )

    var currentNavRoute by rememberSaveable(stateSaver = NavigationBarRoutes.SAVER) {
        mutableStateOf(
            NavigationBarRoutes.Videos
        )
    }

    val currentNavEntries = when (currentNavRoute) {
        NavigationBarRoutes.Videos -> videosNavEntries
        NavigationBarRoutes.Albums -> videosNavEntries + albumsNavEntries
    }

    Scaffold(
        bottomBar = {
            val items = listOf(NavigationBarItemInfo.Videos, NavigationBarItemInfo.Albums)
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                items.forEach { item ->
                    val isSelected = currentNavRoute == item.route
                    NavigationBarItem(
                        interactionSource = remember { NoIndicationInteractionSource() },
                        selected = isSelected,
                        label = { Text(item.title) },
                        icon = {
                            Icon(item.icon, contentDescription = null)
                        },
                        onClick = {
                            currentNavRoute = item.route
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Magenta,
                            unselectedIconColor = Color.White,
                            selectedTextColor = Color.Magenta,
                            unselectedTextColor = Color.White,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        SwingEdgeToEdgeEffect(modifier = Modifier.padding(contentPadding)) {
            NavDisplay(
                entries = currentNavEntries,
                onBack = {
                    val currentBackStack = when (currentNavRoute) {
                        NavigationBarRoutes.Videos -> videosBackStack
                        NavigationBarRoutes.Albums -> albumsBackStack
                    }
                    if (currentBackStack.size > 1) {
                        currentBackStack.removeLastOrNull()
                    } else {
                        if (currentBackStack != videosBackStack) {
                            currentNavRoute = NavigationBarRoutes.Videos
                        }
                    }
                }
            )
        }
    }
}
