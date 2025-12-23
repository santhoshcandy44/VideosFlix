package com.flix.videos.ui.app.player

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.flix.videos.R
import com.flix.videos.ui.app.player.service.player.utils.WindowLayoutState
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.service.CMD_STOP_OVERLAY_VIDEO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.player.managers.ServiceMediaControllerManager
import com.flix.videos.ui.app.player.service.player.PipPlayerService
import com.flix.videos.ui.utils.noRippleClickable
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun OverlayVideoPlayerScreen(
    videoInfo: VideoInfo,
    isPlaying: Boolean,
    mediaController: MediaController,
    windowLayoutState: WindowLayoutState,
    modifier : Modifier= Modifier,
    attachToDraggable: (View, () -> WindowLayoutState, () -> Unit) -> Unit,
    updateWindowSize: (Int, Int, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val serviceMediaControllerManager: ServiceMediaControllerManager =
        koinInject<ServiceMediaControllerManager> {
            parametersOf("overlay videos screen")
        }
    var videoWidth by remember { mutableIntStateOf(videoInfo.width) }
    var videoHeight by remember { mutableIntStateOf(videoInfo.height) }
    val textureView = remember { TextureView(context) }
    val latestWindowLayoutState by rememberUpdatedState(windowLayoutState)

    LaunchedEffect(windowLayoutState) {
        updateWindowSize(videoWidth, videoHeight, true)
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                videoWidth = videoSize.width
                videoHeight = videoSize.height
                mediaController.setVideoTextureView(textureView)
                updateWindowSize(videoWidth, videoHeight, false)
            }
        }
        mediaController.addListener(listener)
        onDispose {
            mediaController.removeListener(listener)
        }
    }

    var showControls by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .wrapContentSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .align(Alignment.Center)
                .then(
                    if (videoWidth > 0 && videoHeight > 0) Modifier.aspectRatio(
                        videoWidth.toFloat() / videoHeight.toFloat()
                    ) else Modifier.alpha(1f)
                ),
            factory = {
                textureView.apply {
                    doOnLayout {
                        attachToDraggable(this, { latestWindowLayoutState }, {
                            showControls = !showControls
                        })
                    }
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            val surface = Surface(surfaceTexture)
                            mediaController.setVideoSurface(surface)
                            if (!mediaController.isPlaying && mediaController.playbackState == Player.STATE_IDLE) {
                                mediaController.prepare()
                                mediaController.play()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            mediaController.clearVideoSurface()
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

                    }
                }
            })

        if (videoWidth > 0 && videoHeight > 0) {

            if (showControls) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video_backward),
                        contentDescription = "Previous",
                        tint = Color.White.copy(0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .noRippleClickable {
                                mediaController.seekToPrevious()
                            }
                    )

                    Icon(
                        painter = painterResource(id = if (isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play),
                        contentDescription = "Play / Pause",
                        tint = Color.White.copy(0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .noRippleClickable {
                                if (isPlaying) {
                                    mediaController.pause()
                                } else {
                                    mediaController.play()
                                }
                            }
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_video_forward),
                        contentDescription = "Next",
                        tint = Color.White.copy(0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .noRippleClickable {
                                mediaController.seekToNext()
                            }
                    )
                }
            }

            IconButton(
                onClick = {
                    if (PipPlayerService.isRunning) {
                        serviceMediaControllerManager.getController(null, Bundle.EMPTY){ mediaController , _ ->
                            mediaController.sendCustomCommand(
                                SessionCommand(
                                    CMD_STOP_OVERLAY_VIDEO_PLAYBACK_MODE,
                                    Bundle.EMPTY
                                ),
                                Bundle.EMPTY
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}