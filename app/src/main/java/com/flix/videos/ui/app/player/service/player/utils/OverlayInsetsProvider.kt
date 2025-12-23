package com.flix.videos.ui.app.player.service.overlay

import android.content.Context
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.graphics.Insets
import com.flix.videos.WindowLayoutState

fun getOverlayInsetsAndBounds(
    context: Context
): WindowLayoutState {

    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val metrics = wm.currentWindowMetrics
    val windowInsets = metrics.windowInsets

    val systemBars =
        windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

    val cutout = windowInsets.displayCutout

    val safeLeft = maxOf(systemBars.left, cutout?.safeInsetLeft ?: 0)
    val safeTop = maxOf(systemBars.top, cutout?.safeInsetTop ?: 0)
    val safeRight = maxOf(systemBars.right, cutout?.safeInsetRight ?: 0)
    val safeBottom = maxOf(systemBars.bottom, cutout?.safeInsetBottom ?: 0)

    val combinedInsets = Insets.of(
        safeLeft,
        safeTop,
        safeRight,
        safeBottom
    )

    return WindowLayoutState(
        insets = combinedInsets,
        widthPx = metrics.bounds.width(),
        heightPx = metrics.bounds.height()
    )
}