package com.flix.videos.ui.app.player

import android.app.Activity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun enterFullScreenMode(activity: Activity, enableSwipeUp: Boolean = true) {
    WindowInsetsControllerCompat(
        activity.window,
        activity.window.decorView
    ).apply {
        hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        if (enableSwipeUp)
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun exitFullScreenMode(activity: Activity) {
    WindowInsetsControllerCompat(
        activity.window,
        activity.window.decorView
    ).apply {
        show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}


