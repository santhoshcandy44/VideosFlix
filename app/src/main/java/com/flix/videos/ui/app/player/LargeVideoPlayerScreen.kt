package com.flix.videos.ui.app.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.ui.SubtitleView
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.ExoplayerSeekDirection.SEEK_BACKWARD
import com.flix.videos.ui.app.player.ExoplayerSeekDirection.SEEK_FORWARD
import com.flix.videos.ui.app.player.common.isLandscape
import com.flix.videos.ui.app.player.observables.observeUserLeaveHint
import com.flix.videos.ui.app.player.observables.observeVolumeChanges
import com.flix.videos.ui.app.player.observables.rememberDeviceOrientationFlow
import com.flix.videos.ui.app.player.service.CMD_START_AUDIO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.CMD_START_VIDEO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.player.managers.ServiceMediaControllerManager
import com.flix.videos.ui.app.player.viewmodel.VideoPlayerViewModel
import com.flix.videos.ui.utils.NoIndicationInteractionSource
import com.flix.videos.ui.utils.findActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.absoluteValue

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeVideoPlayerScreen(
    textureView: TextureView,
    volumeKeyChannel: Channel<Int>,
    exoPlayer: MediaController,
    videoWidth: Int,
    videoHeight: Int,
    viewModel: VideoPlayerViewModel,
    onUpdateSubtitleRef: (SubtitleView) -> Unit,
    onPopUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    //Currently Playing Media Info
    val videoParams = viewModel.videoParams
    val currentPlayingVideoInfo by viewModel.currentPlayingVideoInfo.collectAsState()
    val totalDurationMillis = currentPlayingVideoInfo.duration

    val isMuted by viewModel.isMuted.collectAsState()

    val isControlsVisible by viewModel.isControlsVisible.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sliderProgress by viewModel.sliderProgress.collectAsState()
    val currentDurationMillis by viewModel.currentDurationMillis.collectAsState()
    val isLockedOrientation by viewModel.isLockedOrientation.collectAsState()

    val isAudioOnly by viewModel.isAudioOnly.collectAsState()
    val playBackSpeeds = viewModel.playBackSpeeds
    val currentPlayPackSpeed by viewModel.currentPlayPackSpeed.collectAsState()
    val currentRepeatMode by viewModel.currentRepeatMode.collectAsState()
    val currentAudioTrack by viewModel.currentAudioTrack.collectAsState()
    val isSubtitleEnabled by viewModel.isSubtitleEnabled.collectAsState()
    val currentSubtitleTrack by viewModel.currentSubtitleTrack.collectAsState()
    val localSubtitles by viewModel.localSubtitles.collectAsState()
    val currentLocalSubtitle by viewModel.currentLocalSubtitle.collectAsState()
    val isBackgroundVideoPlayModeEnabled by viewModel.isBackgroundVideoPlayModeEnabled.collectAsState()

    //Compose
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    var showSubtitleSettings by remember { mutableStateOf(false) }
    var showMorePlayerSettings by remember { mutableStateOf(false) }

    var volumeChangeState by remember { mutableStateOf(VerticalDragState()) }
    var volumeVerticalDragState by remember { mutableStateOf(VerticalDragState()) }
    var brightnessVerticalDragState by remember { mutableStateOf(VerticalDragState()) }
    var hideVolumeChangeJob by remember { mutableStateOf<Job?>(null) }

    var subtitlePadding by rememberSaveable { mutableIntStateOf(0) }

    //Player Controls
    val isLandscape = isLandscape()
    val thumbSize = DpSize(14.dp, 14.dp)
    val trackHeight = 4.dp
    var doubleTapSeekDirection by rememberSaveable {
        mutableIntStateOf(
            ExoplayerSeekDirection.SEEK_NONE
        )
    }

    //Volume Controls
    val verticalProgressBarSize = DpSize(24.dp, 160.dp)
    val verticalProgressBarHeightPx = with(density) { verticalProgressBarSize.height.toPx() }

    val deviceOrientationFlow = rememberDeviceOrientationFlow()
    val deviceOrientation by deviceOrientationFlow.collectAsState()
    val orientation = configuration.orientation
    var lastOrientation by rememberSaveable { mutableIntStateOf(orientation) }
    val serviceMediaControllerManager: ServiceMediaControllerManager =
        koinInject<ServiceMediaControllerManager>()
    val pipBuilder = viewModel.pipBuilder

    //Permission
    var showOverlayPermissionRequestDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLandscape) {
        if (isLockedOrientation) {
            if (isLandscape)
                enterFullScreenMode(context.findActivity())
            else
                exitFullScreenMode(context.findActivity())
        }
    }

    LifecycleResumeEffect(isLandscape) {
        if (isLockedOrientation) return@LifecycleResumeEffect onPauseOrDispose {}
        if (isLandscape) {
            enterFullScreenMode(context.findActivity())
            viewModel.showControls()
            viewModel.createControlsHideJob(context.findActivity())
        } else {
            exitFullScreenMode(context.findActivity())
            viewModel.cancelControlsHideJob()
            viewModel.showControls()
        }
        onPauseOrDispose {}
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && isLandscape) {
            viewModel.createControlsHideJobIfNot(context.findActivity())
        } else {
            viewModel.cancelControlsHideJob()
            viewModel.showControls()
        }
    }

    LaunchedEffect(deviceOrientation) {
        if (isLockedOrientation) return@LaunchedEffect
        if (deviceOrientation != lastOrientation) {
            context.findActivity().requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (doubleTapSeekDirection != ExoplayerSeekDirection.SEEK_NONE) {
                        doubleTapSeekDirection = ExoplayerSeekDirection.SEEK_NONE
                        viewModel.onFastSeekFinished()
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(volumeKeyChannel) {
        volumeKeyChannel
            .receiveAsFlow()
            .collectLatest {
                volumeChangeState = volumeChangeState.copy(isDragging = true)
                if (volumeChangeState.progress > 0) {
                    if (isMuted)
                        viewModel.setMuted(false)
                }
                hideVolumeChangeJob?.cancel()
                hideVolumeChangeJob = null
                hideVolumeChangeJob = coroutineScope.launch {
                    delay(3000)
                    volumeChangeState = volumeChangeState.copy(isDragging = false)
                }
            }
    }

    //Volume Changes listener
    observeVolumeChanges { _, maxVolume, volume ->
        viewModel.setMuted(volume == 0)
        volumeChangeState =
            volumeChangeState.copy(progress = volume.toFloat() / maxVolume.toFloat())
    }

    //USer leave hint listener
    observeUserLeaveHint {
        if (!isBackgroundVideoPlayModeEnabled && isPlaying && !isAudioOnly)
            context.findActivity().enterPictureInPictureMode(pipBuilder.build())
    }

    val prepareVideoPlaybackMode: () -> Unit = {
        serviceMediaControllerManager.getController(
            null,
            Bundle.EMPTY
        ) { mediaController, _ ->
            mediaController.stop()
            viewModel.saveMediaIemCurrentPosition()
            mediaController.sendCustomCommand(
                SessionCommand(
                    CMD_START_VIDEO_PLAYBACK_MODE,
                    Bundle.EMPTY
                ),
                Bundle().apply {
                    putString("group", videoParams.group)
                    putLong("video_id", currentPlayingVideoInfo.id)
                }
            )
            viewModel.releaseMediaController()
            context.findActivity().finish()
        }
    }

    //Application Draw Overlays Permission
    val drawOverlaysPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(context)) {
                prepareVideoPlaybackMode()
            }
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black), contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val isLeftSide = offset.x < size.width / 2
                            val isBottomAbove30 = offset.y > (size.height * 0.3f)
                            val isBottomBelow30 = offset.y < (size.height * 0.7f)
                            if (isBottomAbove30 && isBottomBelow30) {
                                viewModel.hideControls()
                                if (isLeftSide) {
                                    hideVolumeChangeJob?.cancel()
                                    hideVolumeChangeJob = null
                                    volumeChangeState =
                                        volumeChangeState.copy(isDragging = false)
                                    volumeVerticalDragState = volumeVerticalDragState.copy(
                                        isDragging = true,
                                        progress = getCurrentVolume(context)
                                    )
                                } else {
                                    brightnessVerticalDragState =
                                        brightnessVerticalDragState.copy(
                                            isDragging = true,
                                            progress = getCurrentWindowBrightness(context)
                                        )
                                }
                            }
                        },
                        onDragEnd = {
                            volumeVerticalDragState = volumeVerticalDragState.copy(
                                isDragging = false
                            )
                            brightnessVerticalDragState = brightnessVerticalDragState.copy(
                                isDragging = false
                            )
                        }
                    ) { change, dragAmount ->
                        if (dragAmount.absoluteValue < 3f) return@detectVerticalDragGestures
                        if (volumeVerticalDragState.isDragging) {
                            change.consume()
                            val percent = -dragAmount / verticalProgressBarHeightPx
                            volumeVerticalDragState = volumeVerticalDragState.copy(
                                progress = (volumeVerticalDragState.progress + percent)
                                    .coerceIn(0f, 1f)
                            )
                            setSystemVolume(context, volumeVerticalDragState.progress)
                        }
                        if (brightnessVerticalDragState.isDragging) {
                            change.consume()
                            val percent = -dragAmount / verticalProgressBarHeightPx
                            brightnessVerticalDragState = brightnessVerticalDragState.copy(
                                progress = (brightnessVerticalDragState.progress + percent)
                                    .coerceIn(0f, 1f)
                            )
                            updateBrightness(context, brightnessVerticalDragState.progress)
                        }
                    }
                }
                .pointerInput(isLockedOrientation, isLandscape) {
                    if (isLockedOrientation) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val isLeftSide = offset.x < size.width / 2
                            val isRightSide = offset.x >= size.width / 2
                            if (isLeftSide) {
                                if (exoPlayer.currentPosition > 0) {
                                    doubleTapSeekDirection =
                                        SEEK_BACKWARD
                                    viewModel.seekBackward()
                                }
                            } else if (isRightSide) {
                                if (exoPlayer.currentPosition < totalDurationMillis) {
                                    doubleTapSeekDirection =
                                        SEEK_FORWARD
                                    viewModel.seekForward()
                                }
                            }
                        },
                        onTap = {
                            if (isControlsVisible) {
                                enterFullScreenMode(context.findActivity())
                                viewModel.cancelControlsHideJob()
                                viewModel.hideControls()
                            } else {
                                viewModel.showControls()
                                if (isLandscape)
                                    viewModel.createControlsHideJob(context.findActivity())
                            }
                        },
                        onPress = {
                            viewModel.cancelControlsHideJob()
                        })

                }) {
            if (isAudioOnly) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp),
                    tint = Color.White.copy(0.6f)
                )
            } else {
                AndroidView(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(
                            if (videoWidth > 0 && videoHeight > 0) Modifier.aspectRatio(
                                videoWidth.toFloat() / videoHeight.toFloat()
                            ) else Modifier.alpha(0f)
                        )
                        .onGloballyPositioned { layoutCoordinates ->
                            if (videoWidth > 0 && videoHeight > 0) {
                                val sourceRect =
                                    layoutCoordinates.boundsInWindow().toAndroidRectF().toRect()

                                pipBuilder.setSourceRectHint(sourceRect)

                                val minRatio = 0.418410f
                                val maxRatio = 2.39f

                                val rawRatio = videoWidth.toFloat() / videoHeight.toFloat()
                                val clamped = rawRatio.coerceIn(minRatio, maxRatio)

                                pipBuilder.setAspectRatio(
                                    Rational(
                                        (clamped * 10000).toInt(),
                                        10000
                                    )
                                )
                            }
                        }, factory = {
                        textureView.apply {
                            exoPlayer.setVideoTextureView(this)
                        }
                    }, update = { textureView ->
                        if (textureView.isAvailable) {
                            exoPlayer.setVideoTextureView(textureView)
                        }
                    })

                SubTitleView(
                    onSetView = {
                        onUpdateSubtitleRef(this)
                    },
                    modifier = Modifier.then(
                        if (isControlsVisible) Modifier.padding(
                            bottom = with(density) {
                                subtitlePadding.toDp()
                            }) else Modifier
                    )
                )
            }
        }

        if (isLockedOrientation) {
            LockedButton(
                onClick = {
                    viewModel.showControls()
                    if (isLandscape)
                        viewModel.createControlsHideJob(context.findActivity())
                    viewModel.updateLockedOrientation(false)
                    context.findActivity().requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            )
        }

        PlayerTopBar(
            title = currentPlayingVideoInfo.title,
            isControlsVisible = isControlsVisible,
            onPopUp = onPopUp,
            onSubTitleSettingsClick = {
                showSubtitleSettings = true
            }) {
            showMorePlayerSettings = true
        }

        VerticalDragController(
            verticalProgressBarSize = verticalProgressBarSize,
            volumeChangeState = volumeChangeState,
            volumeVerticalDragState = volumeVerticalDragState,
            brightnessVerticalDragState = brightnessVerticalDragState
        )

        if (isLandscape()) {
            PlayerControlsLandscape(
                isVisible = isControlsVisible,
                isPlaying = isPlaying,
                isMuted = isMuted,
                sliderProgress = sliderProgress,
                totalDurationMillis = totalDurationMillis,
                currentDurationMillis = currentDurationMillis,
                thumbSize = thumbSize,
                trackHeight = trackHeight,
                orientation = orientation,
                onSeekPrevious = viewModel::seekToPrevious,
                onSeekNext = viewModel::seekToNext,
                onPlayPauseToggle = viewModel::togglePlayPause,
                onMuteToggle = viewModel::toggleMute,
                onEnterPip = {
                    if (isBackgroundVideoPlayModeEnabled) {
                        if (!Settings.canDrawOverlays(context)) {

                            showOverlayPermissionRequestDialog = true
                        } else
                            prepareVideoPlaybackMode()
                    } else {
                        viewModel.hideControls()
                        context.findActivity().enterPictureInPictureMode(pipBuilder.build())
                    }
                },
                onEnterBackgroundAudioPlayMode = {
                    serviceMediaControllerManager.getController(
                        null,
                        Bundle.EMPTY
                    ) { mediaController, _ ->
                        mediaController.stop()
                        viewModel.saveMediaIemCurrentPosition()
                        val allVideos = viewModel.allVideos
                        val playItemIndex = viewModel.findVideoIndexById(
                            allVideos,
                            currentPlayingVideoInfo.id
                        ).let { if (it == -1) 0 else it }
                        val currentPlayingVideoInfo2 = allVideos.getOrElse(
                            playItemIndex
                        ) { VideoInfo.EMPTY }

                        val mediaItems = allVideos.map { videoInfo ->
                            MediaItem.Builder()
                                .setUri(videoInfo.uri)
                                .setMediaId(videoInfo.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(videoInfo.title)
                                        .build()
                                )
                                .build()
                        }
                        mediaController.setMediaItems(
                            mediaItems,
                            playItemIndex,
                            viewModel.getMediaIemLastPosition(currentPlayingVideoInfo2.uri)
                        )
                        mediaController.prepare()
                        mediaController.play()
                        mediaController.sendCustomCommand(
                            SessionCommand(
                                CMD_START_AUDIO_PLAYBACK_MODE,
                                Bundle.EMPTY
                            ),
                            Bundle.EMPTY
                        )
                        viewModel.releaseMediaController()
                        context.findActivity().finish()
                    }
                },
                onLockOrientation = {
                    viewModel.cancelControlsHideJob()
                    viewModel.hideControls()
                    viewModel.updateLockedOrientation(true)
                },
                onRotateOrientation = { newOrientation, config ->
                    lastOrientation = config
                    context.findActivity().requestedOrientation = newOrientation
                },

                onSliderChange = {
                    viewModel.cancelControlsHideJob()
                    viewModel.onSliderValueChange(it)
                    viewModel.onUpdateSliderValueChange(true)
                },
                onSliderFinished = {
                    if (isPlaying)
                        viewModel.cancelControlsHideJob()
                    viewModel.onSliderValueChangeFinished()
                    viewModel.onUpdateSliderValueChange(false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .onGloballyPositioned {
                        subtitlePadding = it.size.height
                    }
            )
        } else {
            PlayerControlsPortrait(
                isVisible = isControlsVisible,
                sliderProgress = sliderProgress,
                totalDurationMillis = totalDurationMillis,
                currentDurationMillis = currentDurationMillis,
                isPlaying = isPlaying,
                thumbSize = thumbSize,
                trackHeight = trackHeight,
                onSliderChange = {
                    viewModel.cancelControlsHideJob()
                    viewModel.onSliderValueChange(it)
                    viewModel.onUpdateSliderValueChange(true)
                },
                onSliderChangeFinished = {
                    if (isPlaying)
                        viewModel.cancelControlsHideJob()
                    viewModel.onSliderValueChangeFinished()
                    viewModel.onUpdateSliderValueChange(false)
                },
                onPlayPauseToggle = viewModel::togglePlayPause,
                onSeekPrevious = viewModel::seekToPrevious,
                onSeekNext = viewModel::seekToNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .onGloballyPositioned {
                        subtitlePadding = it.size.height
                    }
            )
        }

        TapToSeekController(doubleTapSeekDirection)

        if (showMorePlayerSettings) {
            PlayerSettingsMenu(
                isAudioOnly = isAudioOnly,
                isBackgroundVideoPlayModeEnabled = isBackgroundVideoPlayModeEnabled,
                playBackSpeeds = playBackSpeeds,
                currentPlayBackSpeed = currentPlayPackSpeed,
                currentRepeatMode = currentRepeatMode,
                audioTracks = currentPlayingVideoInfo.audioTrackInfos,
                currentAudioTrack = currentAudioTrack,
                onDismiss = { showMorePlayerSettings = false },
                onBackgroundPlayModeChanged = viewModel::toggleBackgroundVideoPlayModeEnabled,
                onToggleAudioOnly = viewModel::toggleAudioOnly,
                onSpeedSelected = viewModel::setCurrentPlayBackSpeed,
                onRepeatModeSelected = viewModel::setCurrentPlayListRepeatMode,
                onAudioSelected = {
                    viewModel.switchAudioTrack(it.groupIndex, it.trackIndex)
                }
            )
        }

        if (showSubtitleSettings) {
            SubTitleSettingsMenu(
                isSubtitleEnabled = isSubtitleEnabled,
                subtitleTracks = currentPlayingVideoInfo.subtitleTrackInfos,
                currentSubtitleTrack = currentSubtitleTrack,
                localSubtitles = localSubtitles,
                currentLocalSubtitle = currentLocalSubtitle,
                onDismiss = { showSubtitleSettings = false },
                onSubtitleSelected = viewModel::switchSubTitleTrack,
                onLocalSubtitleSelected = viewModel::updateCurrentLocalSubtitle,
                onSubtitleToggle = viewModel::onSubtitleToggle
            )
        }

        if (showOverlayPermissionRequestDialog) {
            OverlayPermissionRequestDialog(onAllowClick = {
                drawOverlaysPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$context.packageName".toUri()
                    )
                )
            }, onClose = {
                showOverlayPermissionRequestDialog = false
            })
        }
    }
}

@Composable
private fun OverlayPermissionRequestDialog(
    onAllowClick: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { NoIndicationInteractionSource() }
            ) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints (
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val dialogWidth = when {
                maxWidth < 600.dp -> {
                    // 📱 Phones
                    320.dp
                }

                maxWidth < 840.dp -> {
                    // 📟 Tablets
                    560.dp
                }

                else -> {
                    // 🖥 Large screens (cap)
                    560.dp
                }
            }

            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = dialogWidth),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                interactionSource = remember { NoIndicationInteractionSource() },
                onClick = {}
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "To display content over other apps, please enable the Overlay Permission in settings. This is required for the app to function properly.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        TextButton(
                            onClick = onAllowClick
                        ) {
                            Text("Allow")
                        }

                        TextButton(
                            onClick = onClose
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}