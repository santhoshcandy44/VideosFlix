package com.flix.videos.ui.app.player.service.player.utils

import androidx.core.graphics.Insets

data class WindowLayoutState(
    val insets: Insets = Insets.NONE,
    val widthPx: Int = 0,
    val heightPx: Int = 0
) {
    companion object {
        val NONE = WindowLayoutState()
    }

    val horizontalInsetsPx = insets.left + insets.right
    val verticalInsetsPx = insets.top + insets.bottom
}