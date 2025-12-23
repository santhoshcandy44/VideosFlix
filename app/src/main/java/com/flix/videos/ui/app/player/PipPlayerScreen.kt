package com.flix.videos.ui.app.player

import android.app.PictureInPictureParams
import android.util.Rational
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.flix.videos.R
import com.flix.videos.ui.app.player.common.observerPipModeChange
import com.flix.videos.ui.app.player.observables.createPipAction
import com.flix.videos.ui.app.player.observables.observePipRemoteActions
import com.flix.videos.ui.app.player.viewmodel.VideoPlayerViewModel
import com.flix.videos.ui.utils.findActivity

@Composable
fun PipPlayerScreen(
    textureView: TextureView,
    mediaController: MediaController,
    videoWidth: Int,
    videoHeight: Int,
    isAudioOnly: Boolean,
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    //Compose
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    //Pip
    val pipBuilder = viewModel.pipBuilder

    val updatePipActions: () -> PictureInPictureParams.Builder = {
        pipBuilder.setActions(buildList {
            add(
                createPipAction(
                    context = context,
                    iconRes = R.drawable.ic_video_backward,
                    title = "Backward",
                    requestCode = EXTRA_CONTROL_BACKWARD,
                    controlType = EXTRA_CONTROL_BACKWARD
                )
            )

            if (mediaController.isPlaying)
                add(
                    createPipAction(
                        context = context,
                        iconRes = R.drawable.ic_video_pause,
                        title = "Pause",
                        requestCode = EXTRA_CONTROL_PAUSE,
                        controlType = EXTRA_CONTROL_PAUSE
                    )
                )
            else
                add(
                    createPipAction(
                        context = context,
                        iconRes = R.drawable.ic_video_play,
                        title = "Play",
                        requestCode = EXTRA_CONTROL_PLAY,
                        controlType = EXTRA_CONTROL_PLAY
                    )
                )

            add(
                createPipAction(
                    context = context,
                    iconRes = R.drawable.ic_video_forward,
                    title = "Forward",
                    requestCode = EXTRA_CONTROL_FORWARD,
                    controlType = EXTRA_CONTROL_FORWARD
                )
            )
        })
    }

    //Player Listener
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                context.findActivity().setPictureInPictureParams(updatePipActions().build())
            }
        }
        mediaController.addListener(listener)
        onDispose {
            mediaController.removeListener(listener)
            mediaController.clearVideoTextureView(textureView)
        }
    }

    //Pip Close Listener
    observerPipModeChange { pipModeInfo ->
        if (!pipModeInfo.isInPictureInPictureMode && lifecycleOwner.lifecycle.currentState == Lifecycle.State.CREATED){
            context.findActivity().finish()
        }
    }

    //Pip Remote Actions
    observePipRemoteActions { intent ->
        if ((intent == null) || (intent.action != ACTION_BROADCAST_CONTROL)) {
            return@observePipRemoteActions
        }
        when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
            EXTRA_CONTROL_PAUSE -> {
                mediaController.pause()
            }

            EXTRA_CONTROL_PLAY -> {
                mediaController.play()
            }

            EXTRA_CONTROL_FORWARD -> {
                mediaController.seekToNextMediaItem()
            }

            EXTRA_CONTROL_BACKWARD -> {
                mediaController.seekToPreviousMediaItem()
            }
        }
    }

    Box(
        modifier = modifier
            .wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
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
                            context.findActivity()
                                .setPictureInPictureParams(updatePipActions().build())
                        }
                    },
                factory = {
                    textureView.apply {
                        mediaController.setVideoTextureView(this)
                    }
                }, update = { textureView ->
                    if (textureView.isAvailable) {
                        mediaController.setVideoTextureView(textureView)
                    }
                })
        }
    }
}