package com.flix.videos.ui.app.player

import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView

@OptIn(UnstableApi::class)
@Composable
fun SubTitleView(onSetView:SubtitleView.()-> Unit){
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            SubtitleView(context).apply {
                onSetView()
                setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }
        }
    )
}