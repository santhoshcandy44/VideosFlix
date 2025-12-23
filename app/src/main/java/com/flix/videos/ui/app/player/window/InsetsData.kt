package com.flix.videos.ui.app.player.window


data class InsetsData(
    val leftPx: Int = 0,
    val topPx: Int = 0,
    val rightPx: Int = 0,
    val bottomPx: Int = 0
) {
    val horizontalPx: Int
        get() = leftPx + rightPx

    val verticalPx: Int
        get() = topPx + bottomPx
}
