package com.flix.videos.ui.app.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.ScreenRotation
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
import com.flix.videos.ui.utils.NoIndicationInteractionSource
import com.flix.videos.ui.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.PlayerControlsLandscape(
    isVisible: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    sliderProgress: Float,
    totalDurationMillis: Long,
    currentDurationMillis: Long,
    thumbSize: DpSize,
    trackHeight: Dp,
    orientation: Int,
    lastOrientation: Int,
    pipEnabled: Boolean = true,
    onSeekPrevious: () -> Unit,
    onSeekNext: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onEnterPip: () -> Unit,
    onLockOrientation: () -> Unit,
    onRotateOrientation: (newOrientation: Int, configOrientation: Int) -> Unit,
    onSliderChange: (Float) -> Unit,
    onSliderFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 100)
        ), exit = fadeOut(
            animationSpec = tween(durationMillis = 500)
        ), modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .align(Alignment.Center)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSeekPrevious,
                    modifier = Modifier
                        .size(60.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video_backward),
                        contentDescription = "Rewind 10s",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        painter = if (isPlaying) painterResource(R.drawable.ic_video_pause) else painterResource(
                            R.drawable.ic_video_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = onSeekNext,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video_forward),
                        contentDescription = "Forward 10s",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMuteToggle,
                        interactionSource = remember { NoIndicationInteractionSource() }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute"
                        )
                    }

                    if (isPlaying) {
                        IconButton(
                            onClick = onEnterPip,
                            interactionSource = remember { NoIndicationInteractionSource() }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureInPictureAlt,
                                contentDescription = "Enter PiP mode"
                            )
                        }
                    }

                    IconButton(
                        onClick = onLockOrientation,
                        interactionSource = remember { NoIndicationInteractionSource() }) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Orientation rotate",
                        )
                    }

                    IconButton(
                        onClick = {
                            val (newOrientation, newConfigOrientation) = when (orientation) {
                                Configuration.ORIENTATION_LANDSCAPE ->
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT to Configuration.ORIENTATION_PORTRAIT

                                Configuration.ORIENTATION_PORTRAIT ->
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE

                                else ->
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED to Configuration.ORIENTATION_UNDEFINED
                            }
                            onRotateOrientation(newOrientation, newConfigOrientation)
                        },
                        interactionSource = remember { NoIndicationInteractionSource() }) {
                        Icon(
                            imageVector = Icons.Outlined.ScreenRotation,
                            contentDescription = "Orientation rotate",
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        color = Color.White,
                        text = formatTimeSeconds(totalDurationMillis / 1000f)
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
                        onValueChangeFinished = onSliderFinished,
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
            }
        }
    }
}