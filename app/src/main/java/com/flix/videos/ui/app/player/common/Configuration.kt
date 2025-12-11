package com.flix.videos.ui.app.player.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun isLandscape(): Boolean {
    val config = LocalConfiguration.current
    return config.orientation == Configuration.ORIENTATION_LANDSCAPE
}
