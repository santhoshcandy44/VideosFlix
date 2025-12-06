package com.flix.videos.ui.app.bottombar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationBarItemInfo(
    val route: NavigationBarRoutes,
    val title: String, val icon: ImageVector
) {
    data object Videos :
        NavigationBarItemInfo(NavigationBarRoutes.Videos, "Videos", Icons.Outlined.VideoLibrary)

    data object Albums :
        NavigationBarItemInfo(NavigationBarRoutes.Albums, "Albums", Icons.Outlined.Folder)
}