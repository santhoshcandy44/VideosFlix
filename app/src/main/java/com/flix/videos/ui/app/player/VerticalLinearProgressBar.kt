package com.flix.videos.ui.app.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

@Composable
fun VerticalLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.Magenta.copy(0.6f)
) {
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .border(2.dp, Color.White, RectangleShape)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(progress)
                .align(Alignment.BottomCenter)
                .background(trackColor)
        )
    }
}
