package com.flix.videos.ui.app.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.util.Rational
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.graphics.toRect
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.flix.videos.R
import com.flix.videos.ui.app.player.observables.createPipAction
import com.flix.videos.ui.app.player.observables.observePipRemoteActions
import com.flix.videos.ui.app.player.observables.observeSystemVolume
import com.flix.videos.ui.app.player.observables.observeUserLeaveHint
import com.flix.videos.ui.app.player.observables.observerLifeCycleEvent
import com.flix.videos.ui.app.player.observables.rememberDeviceOrientationFlow
import com.flix.videos.ui.app.player.viewmodel.ExoplayerSeekDirection
import com.flix.videos.ui.app.player.viewmodel.ExoplayerSeekDirection.SEEK_BACKWARD
import com.flix.videos.ui.app.player.viewmodel.ExoplayerSeekDirection.SEEK_FORWARD
import com.flix.videos.ui.app.player.viewmodel.VideoPlayerViewModel
import com.flix.videos.ui.utils.FormatterUtils.formatTimeSeconds
import com.flix.videos.ui.utils.NoIndicationInteractionSource
import com.flix.videos.ui.utils.findActivity
import com.flix.videos.ui.utils.shortToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Constant for broadcast receiver
const val ACTION_BROADCAST_CONTROL = "PRIVATE_PLAYER_BROADCAST"

// Intent extras for broadcast controls from Picture-in-Picture mode.
const val EXTRA_CONTROL_TYPE = "control_type"
const val EXTRA_CONTROL_PLAY = 1
const val EXTRA_CONTROL_PAUSE = 2

//Close Player
const val EXTRA_CONTROL_CLOSE = 3

data class VerticalDragState(
    val isDragging: Boolean = false,
    val progress: Float = 0f
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    onPopUp: () -> Unit, viewModel: VideoPlayerViewModel, modifier: Modifier = Modifier
) {
    val title = viewModel.title
    val videoUri = viewModel.videoUri
    val videoWidth = viewModel.videoWidth
    val videoHeight = viewModel.videoHeight
    val totalDurationMillis = viewModel.totalDurationMillis

    val exoPlayer = viewModel.exoPlayer
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sliderProgress by viewModel.sliderProgress.collectAsState()
    val currentDurationMillis by viewModel.currentDurationMillis.collectAsState()
    val isControlsVisible by viewModel.isControlsVisible.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isLockedOrientation by viewModel.isLockedOrientation.collectAsState()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val deviceOrientationFlow = rememberDeviceOrientationFlow()
    val deviceOrientation by deviceOrientationFlow.collectAsState()

    var lastOrientation by rememberSaveable { mutableStateOf(orientation) }
    var doubleTapSeekDirection by rememberSaveable {
        mutableIntStateOf(
            ExoplayerSeekDirection.SEEK_NONE
        )
    }

    val textureView = remember { TextureView(context) }
    val thumbSize = DpSize(14.dp, 14.dp)
    val trackHeight = 4.dp
    val interactionSource = remember { MutableInteractionSource() }

    var autoHideJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun cancelControlsHideJob() {
        autoHideJob?.cancel()
        autoHideJob = null
    }

    fun createControlsHideJob(timeMillis: Long = 5000) {
        autoHideJob = scope.launch {
            delay(timeMillis)
            enterFullScreenMode(context.findActivity())
            viewModel.hideControls()
        }
    }

    fun scheduleControlsHideJob() {
        cancelControlsHideJob()
        createControlsHideJob()
    }

    fun showPlayerControls() {
        viewModel.showControls()
        scheduleControlsHideJob()
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
        showPlayerControls()
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
                exoPlayer.pause()
            }

            else -> {}
        }
    }

    LaunchedEffect(isPlaying) {
        if (isInPipMode) {
            context.findActivity().setPictureInPictureParams(updatePipActions().build())
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {/*  override fun onVideoSizeChanged(videoSize: VideoSize) {
                  if (videoSize.width == 0 || videoSize.height == 0) return
                  val rotation = exoPlayer.videoFormat?.rotationDegrees ?: 0
                  val correctedWidth =
                      if (rotation == 90 || rotation == 270) videoSize.height else videoSize.width
                  val correctedHeight =
                      if (rotation == 90 || rotation == 270) videoSize.width else videoSize.height

                  videoWidth = correctedWidth
                  videoHeight = correctedHeight
              }*/

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
//                    totalDurationMillis = exoPlayer.duration
                }
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.stopUpdatingProgress()
                    viewModel.onUpdateCurrentDurationMillis(totalDurationMillis)
                    viewModel.onUpdateCurrentDurationMillis(0)
                    exoPlayer.stop()
                    exoPlayer.playWhenReady = false
                    textureView.keepScreenOn = false
                    exoPlayer.seekTo(0)
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
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                shortToast(context, "Can't play this video, open with other app")
                MediaScannerConnection.scanFile(
                    context, arrayOf(videoUri.path), null
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
    var volumeVerticalDragState by remember { mutableStateOf(VerticalDragState()) }
    var brightnessVerticalDragState by remember { mutableStateOf(VerticalDragState()) }

    observeSystemVolume { _, volume ->
        viewModel.setMuted(volume == 0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isInPipMode) {
            AndroidView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(
                        if (videoWidth > 0 && videoHeight > 0) Modifier.aspectRatio(
                            videoWidth.toFloat() / videoHeight.toFloat()
                        ) else Modifier.size(
                            0.dp
                        )
                    ), factory = {
                    exoPlayer.setVideoTextureView(textureView)
                    textureView
                }, update = { textureView ->
                    if (textureView.isAvailable) {
                        exoPlayer.setVideoTextureView(textureView)
                    }
                })
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                val isLeftSide = offset.x < size.width / 2
                                val isBottom70 = offset.y > (size.height * 0.3f)
                                if(isBottom70){
                                    viewModel.hideControls()
                                    if(isLeftSide){
                                        volumeVerticalDragState = volumeVerticalDragState.copy(
                                            isDragging = true,
                                            progress = getCurrentVolume(context)
                                        )
                                    }else{
                                        brightnessVerticalDragState = brightnessVerticalDragState.copy(
                                            isDragging = true,
                                            progress = getCurrentWindowBrightness(context)
                                        )
                                    }
                                }
                            },
                            onDragEnd = {
                                volumeVerticalDragState = volumeVerticalDragState.copy(
                                    isDragging = false,
                                )
                                brightnessVerticalDragState = brightnessVerticalDragState.copy(
                                    isDragging = false,
                                )
                            }
                        ) { change, dragAmount ->
                            if(volumeVerticalDragState.isDragging){
                                change.consume()
                                val percent = -dragAmount / verticalProgressBarHeightPx
                                volumeVerticalDragState = volumeVerticalDragState.copy(
                                    progress = (volumeVerticalDragState.progress + percent)
                                        .coerceIn(0f, 1f)
                                )
                                setSystemVolume(context, volumeVerticalDragState.progress)
                            }
                            if(brightnessVerticalDragState.isDragging){
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
                    .pointerInput(isLockedOrientation) {
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
                                    showPlayerControls()
                                }
                            }, onPress = {
                                cancelControlsHideJob()
                            })
                        }
                    }) {
                AndroidView(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(
                            if (videoWidth > 0 && videoHeight > 0) Modifier.aspectRatio(
                                videoWidth.toFloat() / videoHeight.toFloat()
                            ) else Modifier.size(
                                0.dp
                            )
                        )
                        .onGloballyPositioned { layoutCoordinates ->
                            val sourceRect =
                                layoutCoordinates.boundsInWindow().toAndroidRectF().toRect()
                            pipBuilder.setSourceRectHint(sourceRect)

                            val minRatio = 0.418410f
                            val maxRatio = 2.39f

                            val rawRatio = videoWidth.toFloat() / videoHeight.toFloat()
                            val clamped = rawRatio.coerceIn(minRatio, maxRatio)

                            pipBuilder.setAspectRatio(Rational((clamped * 10000).toInt(), 10000))
                            updatePipActions()
                        }, factory = {
                        exoPlayer.setVideoTextureView(textureView)
                        textureView
                    }, update = { textureView ->
                        if (textureView.isAvailable) {
                            exoPlayer.setVideoTextureView(textureView)
                        }
                    })
            }

            if (isLockedOrientation) {
                IconButton(
                    onClick = {
                        viewModel.updateLockedOrientation(false)
                        showPlayerControls()
                        context.findActivity().requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    interactionSource = remember { NoIndicationInteractionSource() },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Orientation rotate",
                    )
                }
            }

            if (doubleTapSeekDirection == SEEK_BACKWARD) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .background(
                            color = Color.Black.copy(0.7f),
                            shape = RoundedCornerShape(
                                topEnd = 80.dp,
                                bottomEnd = 80.dp
                            )
                        )
                        .align(Alignment.CenterStart)
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Replay10,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("-10s")
                    }
                }
            } else if (doubleTapSeekDirection == SEEK_FORWARD) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .background(
                            color = Color.Black.copy(0.7f),
                            shape = RoundedCornerShape(
                                topStart = 80.dp,
                                bottomStart = 80.dp
                            )
                        )
                        .align(Alignment.CenterEnd)
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Forward10,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("10s")
                    }
                }
            }

            AnimatedVisibility(
                visible = isControlsVisible, enter = fadeIn(
                    animationSpec = tween(durationMillis = 100)
                ), exit = fadeOut(
                    animationSpec = tween(durationMillis = 500)
                ), modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onPopUp) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }, title = {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }

            if (volumeVerticalDragState.isDragging || brightnessVerticalDragState.isDragging) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart)
                ) {
                    listOf(0, 1).forEach { index ->
                        Box(modifier = Modifier.weight(1f)) {
                            if(volumeVerticalDragState.isDragging && index == 0){
                                Column (
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
                                    if(percent > 0){
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
                                    }else{
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                            contentDescription = "Muted",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            if (index == 1 && brightnessVerticalDragState.isDragging) {
                                Column (
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Center),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    VerticalLinearProgressBar(
                                        progress = brightnessVerticalDragState.progress,
                                        trackColor = Color.Yellow.copy(0.6f) ,
                                        modifier = Modifier
                                            .width(verticalProgressBarSize.width)
                                            .height(verticalProgressBarSize.height),
                                    )
                                    val percent = (brightnessVerticalDragState.progress * 100).toInt()
                                    if(percent > 0){
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
                                    }else{
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

            if (isLandscape()) {
                AnimatedVisibility(
                    visible = isControlsVisible,
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
                                onClick = viewModel::seekBackward, modifier = Modifier
                                    .size(60.dp)
                                    .alignByBaseline()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_video_backward),
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            IconButton(
                                onClick = viewModel::togglePlayPause,
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
                                onClick = viewModel::seekForward,
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
                                    onClick = viewModel::toggleMute,
                                    interactionSource = remember { NoIndicationInteractionSource() }) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = if (isMuted) "Unmute" else "Mute"
                                    )
                                }

                                if (isPlaying) {
                                    IconButton(
                                        onClick = {
                                            viewModel.hideControls()
                                            context.findActivity()
                                                .enterPictureInPictureMode(pipBuilder.build())
                                        },
                                        interactionSource = remember { NoIndicationInteractionSource() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PictureInPictureAlt,
                                            contentDescription = "Enter PiP mode"
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.hideControls()
                                        viewModel.updateLockedOrientation(true)
                                        context.findActivity().requestedOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_LOCKED
                                    },
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

                                        lastOrientation = newConfigOrientation
                                        context.findActivity().requestedOrientation = newOrientation
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
                                    onValueChange = {
                                        cancelControlsHideJob()
                                        viewModel.onSliderValueChange(it)
                                        viewModel.onUpdateSliderValueChange(true)
                                    },
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
                                    onValueChangeFinished = {
                                        scheduleControlsHideJob()
                                        viewModel.onSliderValueChangeFinished()
                                        viewModel.onUpdateSliderValueChange(false)
                                    },
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
            } else {
                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 500)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                color = Color.White,
                                text = formatTimeSeconds(totalDurationMillis / 1000f),
                            )

                            Slider(
                                value = sliderProgress,
                                onValueChange = {
                                    cancelControlsHideJob()
                                    viewModel.onSliderValueChange(it)
                                    viewModel.onUpdateSliderValueChange(true)
                                },
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
                                onValueChangeFinished = {
                                    scheduleControlsHideJob()
                                    viewModel.onSliderValueChangeFinished()
                                    viewModel.onUpdateSliderValueChange(false)
                                },
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
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = viewModel::seekBackward) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_video_backward),
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White
                                )
                            }

                            IconButton(onClick = viewModel::togglePlayPause) {
                                Icon(
                                    painter = if (isPlaying) painterResource(R.drawable.ic_video_pause) else painterResource(
                                        R.drawable.ic_video_play
                                    ),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White
                                )
                            }

                            IconButton(onClick = viewModel::seekForward) {
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
        }
    }
}

@Composable
fun isLandscape(): Boolean {
    val config = LocalConfiguration.current
    return config.orientation == Configuration.ORIENTATION_LANDSCAPE
}

@Composable
fun rememberIsInPipMode(): Boolean {
    val activity = LocalContext.current.findActivity() as ComponentActivity
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
            pipMode = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(
            observer
        )
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
}