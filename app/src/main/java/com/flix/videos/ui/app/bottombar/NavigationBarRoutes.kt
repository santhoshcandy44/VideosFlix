package com.flix.videos.ui.app.bottombar

import androidx.compose.runtime.saveable.Saver
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class NavigationBarRoutes : NavKey {
    @Serializable
    data object Videos : NavigationBarRoutes()

    @Serializable
    data object Albums : NavigationBarRoutes()

    companion object {
        val SAVER = Saver<NavigationBarRoutes, String>(
            save = { route ->
                when (route) {
                    Videos -> "Videos"
                    Albums -> "Albums"
                }
            },
            restore = { key ->
                when (key) {
                    "Videos" -> Videos
                    "Albums" -> Albums
                    else -> Videos
                }
            }
        )
    }
}

@Serializable
data class GroupVideos(
    val group: String,
    val groupName: String
) : NavKey