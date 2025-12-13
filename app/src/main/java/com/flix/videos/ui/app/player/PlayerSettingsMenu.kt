package com.flix.videos.ui.app.player

import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.flix.videos.ui.app.player.common.isLandscape
import com.flix.videos.ui.app.player.viewmodel.AudioTrackInfo
import com.flix.videos.ui.app.player.viewmodel.SubtitleTrackInfo
import com.flix.videos.ui.utils.noRippleClickable

@Composable
fun PlayerSettingsMenu(
    isAudioOnly: Boolean,
    playBackSpeeds: List<Float>,
    currentPlayBackSpeed: Float,
    currentRepeatMode: ExoPlayerRepeatMode,
    audioTracks: List<AudioTrackInfo>,
    currentAudioTrack: AudioTrackInfo?,
    onDismiss: () -> Unit,
    onToggleAudioOnly: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onRepeatModeSelected: (ExoPlayerRepeatMode) -> Unit,
    onAudioSelected: (AudioTrackInfo) -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noRippleClickable {
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape()) 0.5f else 0.9f)
                    .fillMaxHeight(if (isLandscape()) 0.9f else 0.5f)
                    .background(Color.Black.copy(0.6f))
                    .border(1.dp, Color.Magenta.copy(0.6f))
                    .verticalScroll(rememberScrollState())
                    .noRippleClickable {}
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdditionalSettings(
                    isAudioOnly = isAudioOnly,
                    onAudioOnlSelected = onToggleAudioOnly
                )

                PlayerSpeedSettings(
                    speeds = playBackSpeeds,
                    currentSpeed = currentPlayBackSpeed,
                    onSpeedSelected = onSpeedSelected
                )

                PlayerRepeatSettings(
                    repeatMode = currentRepeatMode,
                    onRepeatModeSelected = onRepeatModeSelected
                )

                AudioTrackSettings(
                    audioTracks = audioTracks,
                    currentTrack = currentAudioTrack,
                    onAudioSelected = onAudioSelected
                )
            }
        }
    }
}
