package com.flix.videos.ui.app.player.service.player.utils

import android.content.ContextWrapper
import androidx.compose.ui.unit.IntSize
import com.flix.videos.ui.app.player.common.dpToPx

fun ContextWrapper.calculateMiniWindowSize(
    windowLayoutState: WindowLayoutState,
    videoWidth: Int,
    videoHeight: Int,
): IntSize {
/*
    val minRatio = 0.418410f
    val maxRatio = 2.39f
*/

    val aspectRatio =
        (videoWidth.toFloat() / videoHeight)/*.coerceIn(minRatio, maxRatio)*/
/*
    val maxWidth = (fullWidthPx * 0.6f).toInt()
    val maxHeight = (fullHeightPx * 0.45f).toInt()*/

    val margin = dpToPx(8) * 2

    val maxWidth = ((windowLayoutState.widthPx  - windowLayoutState.horizontalInsetsPx - margin) * 0.95f).toInt()
    val maxHeight = ((windowLayoutState.heightPx - windowLayoutState.verticalInsetsPx - margin) * 0.6f).toInt()

    var width = videoWidth
    var height = videoHeight

    if (width > maxWidth) {
        width = maxWidth
        height = (width / aspectRatio).toInt()
    }

    if (height > maxHeight) {
        height = maxHeight
        width = (height * aspectRatio).toInt()
    }
    return IntSize(width, height)
}