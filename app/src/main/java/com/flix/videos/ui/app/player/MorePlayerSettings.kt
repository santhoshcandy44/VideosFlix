package com.flix.videos.ui.app.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.flix.videos.ui.app.player.viewmodel.AudioTrackInfo
import com.flix.videos.ui.app.player.viewmodel.SubtitleTrackInfo
import com.flix.videos.ui.utils.NoIndicationInteractionSource

@Composable
fun AdditionalSettings(
    isAudioOnly: Boolean,
    onAudioOnlSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Additional Settings",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyLarge
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            AdditionalSettingsItem(
                label = "Audio Only",
                icon = Icons.Default.Audiotrack,
                selected = isAudioOnly,
                onClick = onAudioOnlSelected
            )
        }
    }
}

@Composable
private fun AdditionalSettingsItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(
                interactionSource = remember { NoIndicationInteractionSource() },
                indication = null
            ) {
                onClick()
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.Yellow else Color.White,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = label,
            color = if (selected) Color.Yellow else Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PlayerSpeedSettings(
    speeds: List<Float>,
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Playback Speed",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyLarge
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speeds.forEach { speed ->
                val isSelected = speed == currentSpeed
                Text(
                    text = if (speed == 1f) "Normal" else "${speed}X",
                    color = if (isSelected) Color.Yellow else Color.White,
                    modifier = Modifier
                        .clickable(interactionSource = remember { NoIndicationInteractionSource() }) {
                            onSpeedSelected(
                                speed
                            )
                        },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

enum class ExoPlayerRepeatMode {
    REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL, SHUFFLE
}

@Composable
fun PlayerRepeatSettings(
    repeatMode: ExoPlayerRepeatMode,
    onRepeatModeSelected: (ExoPlayerRepeatMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Playback Mode",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyLarge
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            RepeatModeItem(
                label = "Off",
                icon = Icons.Default.Repeat,
                mode = ExoPlayerRepeatMode.REPEAT_MODE_OFF,
                selected = repeatMode == ExoPlayerRepeatMode.REPEAT_MODE_OFF,
                onClick = onRepeatModeSelected
            )

            RepeatModeItem(
                label = "One",
                icon = Icons.Default.RepeatOne,
                mode = ExoPlayerRepeatMode.REPEAT_MODE_ONE,
                selected = repeatMode == ExoPlayerRepeatMode.REPEAT_MODE_ONE,
                onClick = onRepeatModeSelected
            )

            RepeatModeItem(
                label = "All",
                icon = Icons.Default.AllInclusive,
                mode = ExoPlayerRepeatMode.REPEAT_MODE_ALL,
                selected = repeatMode == ExoPlayerRepeatMode.REPEAT_MODE_ALL,
                onClick = onRepeatModeSelected
            )

            RepeatModeItem(
                label = "Shuffle",
                icon = Icons.Default.Shuffle,
                mode = ExoPlayerRepeatMode.SHUFFLE,
                selected = repeatMode == ExoPlayerRepeatMode.SHUFFLE,
                onClick = onRepeatModeSelected
            )
        }
    }
}

@Composable
private fun RepeatModeItem(
    label: String,
    icon: ImageVector,
    mode: ExoPlayerRepeatMode,
    selected: Boolean,
    onClick: (ExoPlayerRepeatMode) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(
                interactionSource = remember { NoIndicationInteractionSource() },
                indication = null
            ) {
                onClick(mode)
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.Yellow else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (selected) Color.Yellow else Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrackSettings(
    audioTracks: List<AudioTrackInfo>,
    currentTrack: AudioTrackInfo?,
    onAudioSelected: (AudioTrackInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Audio Track",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyLarge
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            }) {
            Text(
                text = currentTrack?.label ?: "Select Audio",
                modifier = Modifier
                    .wrapContentWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .drawBehind {
                        val strokeWidthPx = 2.dp.toPx()
                        val width = size.width
                        val height = size.height - strokeWidthPx / 2
                        drawLine(
                            color = Color.Magenta.copy(0.6f),
                            start = Offset(x = 0f, y = height),
                            end = Offset(x = width, y = height),
                            strokeWidth = strokeWidthPx
                        )
                    },
                style = MaterialTheme.typography.bodyMedium
            )

            DropdownMenu(
                modifier = Modifier,
                expanded = expanded,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                audioTracks.forEach { track ->
                    val isSelected = currentTrack?.groupIndex == track.groupIndex &&
                            currentTrack.trackIndex == track.trackIndex
                    DropdownMenuItem(
                        trailingIcon = {
                            if(isSelected){
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        text = {
                            Text(
                                text = track.label ?: "Unknown",
                                color =  Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            expanded = false
                            onAudioSelected(track)
                        },
                        modifier = Modifier
                            .wrapContentWidth()
                    )
                }
            }
        }
    }
}