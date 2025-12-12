package com.flix.videos.ui.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.flix.videos.R
import com.flix.videos.ui.utils.FormatterUtils.formatTimeSeconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControlsPortrait(
    isVisible: Boolean,
    sliderProgress: Float,
    totalDurationMillis: Long,
    currentDurationMillis: Long,
    isPlaying: Boolean,
    thumbSize: DpSize,
    trackHeight: Dp,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSeekNext: () -> Unit,
    onSeekPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 100)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 500)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    color = Color.White,
                    text = formatTimeSeconds(totalDurationMillis / 1000f),
                )

                Slider(
                    value = sliderProgress,
                    onValueChange = onSliderChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .semantics {
                            contentDescription = "Localized Description"
                        }
                        .weight(1f),
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .size(thumbSize)
                                .shadow(1.dp, CircleShape, clip = false)
                                .indication(
                                    interactionSource = interactionSource,
                                    indication = ripple(
                                        bounded = false,
                                        radius = 20.dp
                                    )
                                )
                        )
                    },
                    onValueChangeFinished = onSliderChangeFinished,
                    track = {
                        SliderDefaults.Track(
                            sliderState = it,
                            modifier = Modifier
                                .padding(vertical = 32.dp)
                                .height(trackHeight),
                            thumbTrackGapSize = 0.dp,
                            trackInsideCornerSize = 0.dp,
                            drawStopIndicator = null
                        )
                    })

                Text(
                    color = Color.White,
                    text = formatTimeSeconds(currentDurationMillis / 1000f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSeekPrevious) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video_backward),
                        contentDescription = "Rewind 10s",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onPlayPauseToggle) {
                    Icon(
                        painter = if (isPlaying) painterResource(R.drawable.ic_video_pause) else painterResource(
                            R.drawable.ic_video_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onSeekNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video_forward),
                        contentDescription = "Forward 10s",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
