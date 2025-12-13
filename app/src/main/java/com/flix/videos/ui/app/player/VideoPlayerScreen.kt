package com.flix.videos.ui.app.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaScannerConnection
import android.util.Log
import android.util.Rational
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import com.flix.videos.R
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.ExoplayerSeekDirection.SEEK_BACKWARD
import com.flix.videos.ui.app.player.ExoplayerSeekDirection.SEEK_FORWARD
import com.flix.videos.ui.app.player.common.isLandscape
import com.flix.videos.ui.app.player.common.rememberIsInPipMode
import com.flix.videos.ui.app.player.observables.createPipAction
import com.flix.videos.ui.app.player.observables.observePipRemoteActions
import com.flix.videos.ui.app.player.observables.observeUserLeaveHint
import com.flix.videos.ui.app.player.observables.observeVolumeChanges
import com.flix.videos.ui.app.player.observables.observerLifeCycleEvent
import com.flix.videos.ui.app.player.observables.rememberDeviceOrientationFlow
import com.flix.videos.ui.app.player.viewmodel.AudioTrackInfo
import com.flix.videos.ui.app.player.viewmodel.SubtitleTrackInfo
import com.flix.videos.ui.app.player.viewmodel.VideoPlayerViewModel
import com.flix.videos.ui.utils.findActivity
import com.flix.videos.ui.utils.shortToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.absoluteValue

// Constant for broadcast receiver
const val ACTION_BROADCAST_CONTROL = "PRIVATE_PLAYER_BROADCAST"

// Intent extras for broadcast controls from Picture-in-Picture mode.
const val EXTRA_CONTROL_TYPE = "control_type"
const val EXTRA_CONTROL_PLAY = 1
const val EXTRA_CONTROL_PAUSE = 2

//Close Player
const val EXTRA_CONTROL_CLOSE = 3

data class VerticalDragState(
    val progress: Float = 0f,
    val isDragging: Boolean = false
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    volumeKeyChannel: Channel<Int>,
    onPopUp: () -> Unit, viewModel: VideoPlayerViewModel, modifier: Modifier = Modifier
) {
    val currentPlayingVideoInfo by viewModel.currentPlayingVideoInfo.collectAsState()

    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    val totalDurationMillis = currentPlayingVideoInfo.duration

    val exoPlayer = viewModel.exoPlayer
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sliderProgress by viewModel.sliderProgress.collectAsState()
    val currentDurationMillis by viewModel.currentDurationMillis.collectAsState()
    val isControlsVisible by viewModel.isControlsVisible.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
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

    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val deviceOrientationFlow = rememberDeviceOrientationFlow()
    val deviceOrientation by deviceOrientationFlow.collectAsState()
    var lastOrientation by rememberSaveable { mutableIntStateOf(orientation) }
    var doubleTapSeekDirection by rememberSaveable {
        mutableIntStateOf(
            ExoplayerSeekDirection.SEEK_NONE
        )
    }

    val textureView = remember { TextureView(context) }
    val subtitleViewRef = remember { AtomicReference<SubtitleView?>(null) }

    val thumbSize = DpSize(14.dp, 14.dp)
    val trackHeight = 4.dp

    var autoHideJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val isLandscape = isLandscape()

    var subtitlePadding by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(isLandscape) {
        if (isLandscape)
            enterFullScreenMode(context.findActivity())
    }

    fun cancelControlsHideJob() {
        autoHideJob?.cancel()
        autoHideJob = null
    }

    fun createControlsHideJob(timeMillis: Long = 5000) {
        autoHideJob = coroutineScope.launch {
            delay(timeMillis)
            enterFullScreenMode(context.findActivity())
            viewModel.hideControls()
        }
    }

    fun showPlayerControls(isLandscape: Boolean, scheduleControlsHideJob: Boolean = true) {
        cancelControlsHideJob()
        viewModel.showControls()
        if (!isLandscape)
            exitFullScreenMode(context.findActivity())
        if (scheduleControlsHideJob)
            createControlsHideJob()
    }

    val isInPipMode = rememberIsInPipMode()
    val pipBuilder = viewModel.pipBuilder

    fun updatePipActions(): PictureInPictureParams.Builder {
        val actions = buildList {
            if (exoPlayer.isPlaying) {
                add(
                    createPipAction(
                        context = context,
                        iconRes = R.drawable.ic_video_pause,
                        title = "Pause",
                        requestCode = EXTRA_CONTROL_PAUSE,
                        controlType = EXTRA_CONTROL_PAUSE
                    )
                )
            } else {
                add(
                    createPipAction(
                        context = context,
                        iconRes = R.drawable.ic_video_play,
                        title = "Play",
                        requestCode = EXTRA_CONTROL_PLAY,
                        controlType = EXTRA_CONTROL_PLAY
                    )
                )
            }
        }
        return pipBuilder.setActions(actions)
    }

    LifecycleResumeEffect(Unit) {
        if (isLockedOrientation) return@LifecycleResumeEffect onPauseOrDispose {}
        showPlayerControls(isLandscape)
        onPauseOrDispose {}
    }

    LaunchedEffect(deviceOrientation) {
        if (isLockedOrientation) return@LaunchedEffect
        if (deviceOrientation != lastOrientation) {
            context.findActivity().requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    observerLifeCycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                viewModel.saveMediaIemCurrentPosition()
                exoPlayer.pause()
            }

            else -> {}
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            cancelControlsHideJob()
            showPlayerControls(isLandscape, false)
        } else
            if (autoHideJob == null)
                createControlsHideJob()
        if (isInPipMode) {
            context.findActivity().setPictureInPictureParams(updatePipActions().build())
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                super.onCues(cueGroup)
                subtitleViewRef.get()?.setCues(cueGroup.cues)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                        val previousIndex = exoPlayer.previousMediaItemIndex
                        if (previousIndex != C.INDEX_UNSET) {
                            val prevItem = exoPlayer.getMediaItemAt(previousIndex)
                            val prevUri = prevItem.localConfiguration?.uri
                            if (prevUri != null)
                                viewModel.clearMediaItemPosition(prevUri)
                        }
                    }

                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                        val currentUri = mediaItem?.localConfiguration?.uri
                        if (currentUri != null)
                            viewModel.clearMediaItemPosition(currentUri)
                    }
                }
                (mediaItem?.localConfiguration?.tag as? VideoInfo)?.let { videoInfo ->
                    viewModel.setCurrentPlayingVideoInfo(videoInfo)
                }

                mediaItem?.localConfiguration?.uri?.let { uri ->
                    val pos = viewModel.getMediaIemLastPosition(uri)
                    if (pos > 0L)
                        exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, pos)
                }
                Log.e("PLAYER", "Transition item changed $reason")
            }

            override fun onTracksChanged(tracks: Tracks) {
                super.onTracksChanged(tracks)
                val audioTracks = mutableListOf<AudioTrackInfo>()
                tracks.groups.forEachIndexed { groupIndex, group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (tIndex in 0 until group.length) {
                            val format = group.mediaTrackGroup.getFormat(tIndex)
                            val displayLabel =
                                if (format.language != null && format.label != null) {
                                    "${format.language}_${format.label}"
                                } else {
                                    "Audio Track ${tIndex + 1}"
                                }
                            audioTracks.add(
                                AudioTrackInfo(
                                    groupIndex = groupIndex,
                                    trackIndex = tIndex,
                                    language = format.language,
                                    label = displayLabel
                                )
                            )
                        }
                    }
                }

                val subtitleTrackInfos = mutableListOf<SubtitleTrackInfo>()
                tracks.groups.forEachIndexed { groupIndex, group ->
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        for (trackIndex in 0 until group.length) {
                            val format = group.mediaTrackGroup.getFormat(trackIndex)
                            val displayLabel =
                                if (format.language != null && format.label != null) {
                                    "${format.language}_${format.label}"
                                } else {
                                    "Subtitle ${trackIndex + 1}"
                                }
                            subtitleTrackInfos.add(
                                SubtitleTrackInfo(
                                    groupIndex = groupIndex,
                                    trackIndex = trackIndex,
                                    language = format.language,
                                    label = displayLabel
                                )
                            )
                        }
                    }
                }

                (exoPlayer.currentMediaItem?.localConfiguration?.tag as? VideoInfo)?.let { videoInfo ->
                    viewModel.setCurrentPlayingVideoInfo(
                        videoInfo.copy(
                            audioTrackInfos = audioTracks,
                            subtitleTrackInfos = subtitleTrackInfos
                        )
                    )

                    viewModel.getMediaIemAudioTrack(videoInfo.uri)?.let {
                        viewModel.switchAudioTrack(it.first, it.second)
                    }
                }

                viewModel.setCurrentAudioTrack()
                viewModel.setCurrentSubtitleTrack()
                Log.e("PLAYER", "On track changed")
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                videoWidth = videoSize.width
                videoHeight = videoSize.height
                Log.e("PLAYER", "On video size changed ${videoSize.width}")
            }

            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()
                /*  val size = exoPlayer.videoSize
                  videoWidth = size.width
                  videoHeight = size.height*/
                Log.e("PLAYER", "First frame rendered")
            }

            override fun onVolumeChanged(volume: Float) {
                super.onVolumeChanged(volume)
                viewModel.setMuted(volume == 0f)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_IDLE) {
                    textureView.keepScreenOn = false
                }
                if (playbackState == Player.STATE_BUFFERING) {
                    textureView.keepScreenOn = true
                }
                if (playbackState == Player.STATE_READY) {
                    if (doubleTapSeekDirection != ExoplayerSeekDirection.SEEK_NONE) {
                        doubleTapSeekDirection = ExoplayerSeekDirection.SEEK_NONE
                        viewModel.onFastSeekFinished()
                    }
                    textureView.keepScreenOn = exoPlayer.isPlaying
                }
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.stopUpdatingProgress()
                    viewModel.onUpdateCurrentDurationMillis(totalDurationMillis)
                    viewModel.onUpdateCurrentDurationMillis(0)
                    exoPlayer.stop()
                    exoPlayer.playWhenReady = false
                    textureView.keepScreenOn = false
                    val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
                    if (currentUri != null)
                        viewModel.clearMediaItemPosition(currentUri)
                    val firstUri = exoPlayer.getMediaItemAt(0).localConfiguration?.uri
                    val pos = if (firstUri != null)
                        viewModel.getMediaIemLastPosition(firstUri)
                    else
                        0
                    exoPlayer.seekTo(0, pos)
                    exoPlayer.prepare()
                    viewModel.onSliderValueChange(0f)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isPlayingOnReady = exoPlayer.playWhenReady
                if (isPlayingOnReady)
                    viewModel.startUpdatingProgress()
                else
                    viewModel.stopUpdatingProgress()
                viewModel.onUpdateIsPlaying(isPlayingOnReady)

                if (!isPlaying) {
                    viewModel.saveMediaIemCurrentPosition()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                shortToast(context, "Can't play this video, open with other app")
                MediaScannerConnection.scanFile(
                    context, arrayOf(currentPlayingVideoInfo.uri.path), null
                ) { _, uri ->
                    val shareIntent = Intent(
                        Intent.ACTION_VIEW
                    ).apply {
                        setDataAndType(uri, type)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    if (shareIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(Intent.createChooser(shareIntent, "Open with"))
                    } else {
                        shortToast(context, "No app to play the video")
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    observePipRemoteActions { intent ->
        if ((intent == null) || (intent.action != ACTION_BROADCAST_CONTROL)) {
            return@observePipRemoteActions
        }
        when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
            EXTRA_CONTROL_PAUSE -> {
                exoPlayer.pause()
            }

            EXTRA_CONTROL_PLAY -> {
                exoPlayer.play()
            }

            EXTRA_CONTROL_CLOSE -> {
                context.findActivity().finish()
            }
        }
        context.findActivity().setPictureInPictureParams(updatePipActions().build())
    }

    observeUserLeaveHint {
        if (isPlaying) {
            context.findActivity().enterPictureInPictureMode(pipBuilder.build())
        }
    }

    val density = LocalDensity.current
    val verticalProgressBarSize = DpSize(24.dp, 160.dp)
    val verticalProgressBarHeightPx = with(density) { verticalProgressBarSize.height.toPx() }

    var volumeChangeState by remember { mutableStateOf(VerticalDragState()) }
    var volumeVerticalDragState by remember { mutableStateOf(VerticalDragState()) }
    var brightnessVerticalDragState by remember { mutableStateOf(VerticalDragState()) }
    var hideVolumeChangeJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        volumeKeyChannel
            .receiveAsFlow()
            .collectLatest {
                volumeChangeState = volumeChangeState.copy(isDragging = true)
                if(volumeChangeState.progress > 0){
                    if(isMuted)
                        viewModel.setMuted(false)
                }
                Log.e("Player","Changes ${volumeChangeState.progress}")

                hideVolumeChangeJob?.cancel()
                hideVolumeChangeJob = null
                hideVolumeChangeJob = coroutineScope.launch {
                    delay(3000)
                    volumeChangeState = volumeChangeState.copy(isDragging = false)
                }
            }
    }

    observeVolumeChanges { isInitial, maxVolume, volume ->
        viewModel.setMuted(volume == 0)
        if(!isInitial)
            volumeChangeState = volumeChangeState.copy(progress = volume.toFloat()/maxVolume.toFloat(),)
    }

    var showSubtitleSettings by remember { mutableStateOf(false) }
    var showMorePlayerSettings by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isInPipMode) {
            if (isAudioOnly) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center),
                    tint = Color.White.copy(0.6f)
                )
            } else {
                AndroidView(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(
                            if (videoWidth > 0 && videoHeight > 0) Modifier.aspectRatio(
                                videoWidth.toFloat() / videoHeight.toFloat()
                            ) else Modifier.alpha(
                                0f
                            )
                        ), factory = {
                        exoPlayer.setVideoTextureView(textureView)
                        textureView
                    }, update = { textureView ->
                        if (textureView.isAvailable) {
                            exoPlayer.setVideoTextureView(textureView)
                        }
                    })
            }
        } else {
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
                        if (!isLockedOrientation) {
                            detectTapGestures(onDoubleTap = { offset ->
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
                            }, onTap = {
                                if (isControlsVisible) {
                                    enterFullScreenMode(context.findActivity())
                                    viewModel.hideControls()
                                } else {
                                    showPlayerControls(isLandscape)
                                }
                            }, onPress = {
                                cancelControlsHideJob()
                            })
                        }
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
                                updatePipActions()
                            }, factory = {
                            exoPlayer.setVideoTextureView(textureView)
                            textureView
                        }, update = { textureView ->
                            if (textureView.isAvailable) {
                                exoPlayer.setVideoTextureView(textureView)
                            }
                        })

                    SubTitleView(
                        onSetView = {
                            subtitleViewRef.set(this)
                        },
                        modifier = Modifier.then(
                            if (isControlsVisible) Modifier.padding(
                                bottom = with(
                                    density
                                ) {
                                    subtitlePadding.toDp()
                                }) else Modifier
                        )
                    )
                }
            }

            if (isLockedOrientation) {
                LockedButton(
                    onClick = {
                        viewModel.updateLockedOrientation(false)
                        showPlayerControls(isLandscape)
                        context.findActivity().requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                )
            }

            TapToSeekController(doubleTapSeekDirection)

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
                        viewModel.hideControls()
                        context.findActivity().enterPictureInPictureMode(pipBuilder.build())
                    },
                    onLockOrientation = {
                        viewModel.hideControls()
                        viewModel.updateLockedOrientation(true)
                    },
                    onRotateOrientation = { newOrientation, config ->
                        lastOrientation = config
                        context.findActivity().requestedOrientation = newOrientation
                    },

                    onSliderChange = {
                        cancelControlsHideJob()
                        viewModel.onSliderValueChange(it)
                        viewModel.onUpdateSliderValueChange(true)
                    },
                    onSliderFinished = {
                        if (isPlaying) {
                            createControlsHideJob()
                        }
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
                        cancelControlsHideJob()
                        viewModel.onSliderValueChange(it)
                        viewModel.onUpdateSliderValueChange(true)
                    },
                    onSliderChangeFinished = {
                        if (isPlaying) {
                            createControlsHideJob()
                        }
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

            if (showMorePlayerSettings) {
                PlayerSettingsMenu(
                    isAudioOnly = isAudioOnly,
                    playBackSpeeds = playBackSpeeds,
                    currentPlayBackSpeed = currentPlayPackSpeed,
                    currentRepeatMode = currentRepeatMode,
                    audioTracks = currentPlayingVideoInfo.audioTrackInfos,
                    currentAudioTrack = currentAudioTrack,
                    onDismiss = { showMorePlayerSettings = false },
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
        }
    }
}