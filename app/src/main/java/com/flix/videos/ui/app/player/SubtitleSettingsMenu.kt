package com.flix.videos.ui.app.player

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.flix.videos.ui.app.player.common.isLandscape
import com.flix.videos.ui.app.player.viewmodel.SubtitleTrackInfo
import com.flix.videos.ui.app.viewmodel.SubtitleFileInfo

@Composable
fun SubTitleSettingsMenu(
    isSubtitleEnabled: Boolean,
    onSubtitleToggle:(Boolean) -> Unit,
    subtitleTracks: List<SubtitleTrackInfo>,
    currentSubtitleTrack: SubtitleTrackInfo?,
    localSubtitles: List<SubtitleFileInfo>,
    currentLocalSubtitle: SubtitleFileInfo?,
    onDismiss: () -> Unit,
    onSubtitleSelected: (SubtitleTrackInfo) -> Unit,
    onLocalSubtitleSelected: (SubtitleFileInfo) -> Unit
    ) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(
            flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            excludeFromSystemGesture = true,
        ),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape()) 0.5f else 0.9f)
                .fillMaxHeight(if (isLandscape()) 0.9f else 0.5f)
                .background(Color.Black.copy(0.6f))
                .border(1.dp, Color.Magenta.copy(0.6f))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Subtitles/CC",
                    fontSize = 16.sp,
                    color = Color.White
                )

                Switch(
                    checked = isSubtitleEnabled,
                    onCheckedChange = onSubtitleToggle
                )
            }

            if(isSubtitleEnabled){
                SubTitleSettings(
                    subTitleTracks = subtitleTracks,
                    currentTrack = currentSubtitleTrack,
                    onSubtitleSelected = onSubtitleSelected
                )

                LocalSubtitles(
                    localSubtitles = localSubtitles,
                    currentLocalSubtitle = currentLocalSubtitle,
                            onLocalSubtitleSelected = onLocalSubtitleSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubTitleSettings(
    subTitleTracks: List<SubtitleTrackInfo>,
    currentTrack: SubtitleTrackInfo?,
    onSubtitleSelected: (SubtitleTrackInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose Subtitles/CC",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyLarge
        )

        if(subTitleTracks.isEmpty()){
            Text(
                text = "No subtitle/cc track found.",
                style = MaterialTheme.typography.bodyMedium
            )
        }else{
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = it
                }) {
                Text(
                    text = currentTrack?.label ?: "Select Subtitle/CC",
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
                    subTitleTracks.forEach { track ->
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
                                onSubtitleSelected(track)
                            },
                            modifier = Modifier
                                .wrapContentWidth()
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalSubtitles(
    localSubtitles: List<SubtitleFileInfo>,
    currentLocalSubtitle: SubtitleFileInfo?,
    onLocalSubtitleSelected: (SubtitleFileInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose Local Subtitles/CC",
            color = Color.LightGray,
            style = MaterialTheme.typography.bodyLarge
        )

        if(localSubtitles.isEmpty()){
            Text(
                text = "No local subtitles/cc found.",
                style = MaterialTheme.typography.bodyMedium
            )
        }else{
            Column (modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentLocalSubtitle?.name ?: "Select Local Subtitle/CC",
                    modifier = Modifier
                        .wrapContentWidth()
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
                        }.clickable{
                            expanded = !expanded
                        },
                    style = MaterialTheme.typography.bodyMedium
                )

                if(expanded){
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(localSubtitles) { localSubtitle ->
                            val isSelected = currentLocalSubtitle?.id == localSubtitle.id
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = localSubtitle.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onLocalSubtitleSelected(localSubtitle)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
