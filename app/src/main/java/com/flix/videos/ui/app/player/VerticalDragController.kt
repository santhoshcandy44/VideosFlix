package com.flix.videos.ui.app.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.VerticalDragController(
    verticalProgressBarSize: DpSize,
    volumeChangeState: VerticalDragState,
    volumeVerticalDragState: VerticalDragState,
    brightnessVerticalDragState: VerticalDragState
) {
    val isVolumeActive = volumeChangeState.isDragging || volumeVerticalDragState.isDragging
    val isBrightnessActive = brightnessVerticalDragState.isDragging

    if (isVolumeActive) {
        DragIndicator(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            progressBarSize = verticalProgressBarSize,
            progress = if (volumeChangeState.isDragging)
                volumeChangeState.progress
            else
                volumeVerticalDragState.progress,
            trackColor = Color(0xFF6EE66E).copy(0.6f),
            icon = Icons.AutoMirrored.Filled.VolumeOff,
            percent = ((if (volumeChangeState.isDragging)
                volumeChangeState.progress
            else
                volumeVerticalDragState.progress) * 100).toInt()
        )
    }

    if (isBrightnessActive) {
        DragIndicator(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            progressBarSize = verticalProgressBarSize,
            progress = brightnessVerticalDragState.progress,
            trackColor = Color.Yellow.copy(alpha = 0.6f),
            icon = Icons.Filled.BrightnessLow,
            percent = (brightnessVerticalDragState.progress * 100).toInt()
        )
    }
}

@Composable
private fun DragIndicator(
    modifier: Modifier = Modifier,
    progressBarSize: DpSize,
    progress: Float,
    percent: Int,
    trackColor: Color,
    icon: ImageVector
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VerticalLinearProgressBar(
            progress = progress,
            modifier = Modifier
                .width(progressBarSize.width)
                .height(progressBarSize.height),
            trackColor = trackColor
        )

        if (percent > 0) {
            Text(
                text = "$percent%",
                color = Color.White,
                fontSize = 18.sp,
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}