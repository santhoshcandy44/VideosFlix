package com.flix.videos.ui.app.player.common

import android.content.ContextWrapper


fun ContextWrapper.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}