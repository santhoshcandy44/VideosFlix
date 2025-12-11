package com.flix.videos.ui.app.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.VerticalDragController(
    verticalProgressBarSize: DpSize,
    volumeVerticalDragState: VerticalDragState,
    brightnessVerticalDragState: VerticalDragState
){
    if (volumeVerticalDragState.isDragging || brightnessVerticalDragState.isDragging) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
        ) {
            listOf(0, 1).forEach { index ->
                Box(modifier = Modifier.weight(1f)) {
                    if (volumeVerticalDragState.isDragging && index == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VerticalLinearProgressBar(
                                progress = volumeVerticalDragState.progress,
                                modifier = Modifier
                                    .width(verticalProgressBarSize.width)
                                    .height(verticalProgressBarSize.height),
                                trackColor = Color(0xFF6EE66E).copy(0.6f)
                            )
                            val percent = (volumeVerticalDragState.progress * 100).toInt()

                            if (percent > 0) {
                                Text(
                                    text = "${percent}%",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    style = LocalTextStyle.current.copy(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Muted",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    if (index == 1 && brightnessVerticalDragState.isDragging) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VerticalLinearProgressBar(
                                progress = brightnessVerticalDragState.progress,
                                trackColor = Color.Yellow.copy(0.6f),
                                modifier = Modifier
                                    .width(verticalProgressBarSize.width)
                                    .height(verticalProgressBarSize.height),
                            )
                            val percent =
                                (brightnessVerticalDragState.progress * 100).toInt()
                            if (percent > 0) {
                                Text(
                                    text = "${percent}%",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    style = LocalTextStyle.current.copy(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.BrightnessLow,
                                    contentDescription = "Muted",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}