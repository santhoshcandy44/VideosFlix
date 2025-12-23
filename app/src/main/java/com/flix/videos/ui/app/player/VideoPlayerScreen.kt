package com.flix.videos.ui.app.player

import android.content.Intent
import android.media.MediaScannerConnection
import android.view.TextureView
import android.view.View
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import com.flix.videos.ui.app.player.common.rememberIsInPipMode
import com.flix.videos.ui.app.player.observables.observerLifeCycleEvent
import com.flix.videos.ui.app.player.service.player.utils.WindowLayoutState
import com.flix.videos.ui.app.player.viewmodel.AudioTrackInfo
import com.flix.videos.ui.app.player.viewmodel.SubtitleTrackInfo
import com.flix.videos.ui.app.player.viewmodel.VideoPlayerViewModel
import com.flix.videos.ui.utils.shortToast
import kotlinx.coroutines.channels.Channel
import okhttp3.internal.toLongOrDefault
import java.util.concurrent.atomic.AtomicReference

// Constant for broadcast receiver
const val ACTION_BROADCAST_CONTROL = "PRIVATE_PLAYER_BROADCAST"

// Intent extras for broadcast controls from Picture-in-Picture mode.
const val EXTRA_CONTROL_TYPE = "control_type"
const val EXTRA_CONTROL_PLAY = 1
const val EXTRA_CONTROL_PAUSE = 2

const val EXTRA_CONTROL_FORWARD = 3

const val EXTRA_CONTROL_BACKWARD = 4

data class VerticalDragState(
    val progress: Float = 0f,
    val isDragging: Boolean = false
)


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier,
    volumeKeyChannel: Channel<Int> = Channel(),
    isOverlayWindow: Boolean = false,
    windowLayoutState: WindowLayoutState = WindowLayoutState.NONE,
    onPopUp: () -> Unit = {},
    attachDragBehavior: (View, () -> WindowLayoutState, () -> Unit) -> Unit = { _, _, _ -> },
    updateWindowSize: (Int, Int, Boolean) -> Unit = { _, _, _ -> }
) {
    val mediaSessionControllerState by viewModel.controllerState.collectAsState()
    val mediaController = mediaSessionControllerState ?: return

    val currentPlayingVideoInfo by viewModel.currentPlayingVideoInfo.collectAsState()
    var videoWidth by remember { mutableIntStateOf(currentPlayingVideoInfo.width) }
    var videoHeight by remember { mutableIntStateOf(currentPlayingVideoInfo.height) }
    val totalDurationMillis = currentPlayingVideoInfo.duration

    val isAudioOnly by viewModel.isAudioOnly.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    if (isLoading) return

    val isPlaying by viewModel.isPlaying.collectAsState()

    val context = LocalContext.current

    val textureView = remember { TextureView(context) }
    val subtitleViewRef = remember { AtomicReference<SubtitleView?>(null) }

    //Player Pause When OnStop Occurs
    if (!isOverlayWindow) {
        observerLifeCycleEvent { event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    viewModel.saveMediaIemCurrentPosition()
                    mediaController.pause()
                }
                else -> {}
            }
        }
    }

    //Player Listener
    DisposableEffect(Unit) {
        videoWidth = mediaController.videoSize.width
        videoHeight = mediaController.videoSize.height
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                if (!playWhenReady &&
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
                ) {
                    mediaController.pause()
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                super.onCues(cueGroup)
                subtitleViewRef.get()?.setCues(cueGroup.cues)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                        val previousIndex = mediaController.previousMediaItemIndex
                        if (previousIndex != C.INDEX_UNSET) {
                            val prevItem = mediaController.getMediaItemAt(previousIndex)
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

                mediaItem?.mediaId
                    ?.toLongOrDefault(-1)
                    ?.takeIf { it != -1L }
                    ?.let(viewModel::getCurrentPlayingVideoInfo)
                    ?.let(viewModel::setCurrentPlayingVideoInfo)

                mediaItem?.localConfiguration?.uri?.let { uri ->
                    val pos = viewModel.getMediaIemLastPosition(uri)
                    if (pos > 0L)
                        mediaController.seekTo(mediaController.currentMediaItemIndex, pos)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                super.onTracksChanged(tracks)
                if (tracks.groups.isNotEmpty()) {
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
                    mediaController.currentMediaItem?.mediaId
                        ?.toLongOrDefault(-1)
                        ?.takeIf { it != -1L }
                        ?.let(viewModel::getCurrentPlayingVideoInfo)
                        ?.also { videoInfo ->
                            viewModel.setCurrentPlayingVideoInfo(
                                videoInfo.copy(
                                    audioTrackInfos = audioTracks,
                                    subtitleTrackInfos = subtitleTrackInfos
                                )
                            )

                            viewModel.getMediaIemAudioTrack(videoInfo.uri)
                                ?.let { (groupIndex, trackIndex) ->
                                    viewModel.switchAudioTrack(
                                        groupIndex,
                                        trackIndex
                                    )
                                }
                        }

                    viewModel.setCurrentAudioTrack()
                    viewModel.setCurrentSubtitleTrack()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                videoWidth = videoSize.width
                videoHeight = videoSize.height
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
                    textureView.keepScreenOn = mediaController.isPlaying
                }
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.stopUpdatingProgress()
                    viewModel.onUpdateCurrentDurationMillis(totalDurationMillis)
                    viewModel.onUpdateCurrentDurationMillis(0)
                    mediaController.stop()
                    mediaController.playWhenReady = false
                    textureView.keepScreenOn = false
                    val currentUri = mediaController.currentMediaItem?.localConfiguration?.uri
                    if (currentUri != null)
                        viewModel.clearMediaItemPosition(currentUri)
                    val firstUri = mediaController.getMediaItemAt(0).localConfiguration?.uri
                    val pos = if (firstUri != null)
                        viewModel.getMediaIemLastPosition(firstUri)
                    else
                        0
                    mediaController.seekTo(0, pos)
                    mediaController.prepare()
                    viewModel.onSliderValueChange(0f)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isPlayingOnReady = isPlaying || mediaController.playWhenReady
                if (isPlayingOnReady)
                    viewModel.startUpdatingProgress()
                else
                    viewModel.stopUpdatingProgress()
                viewModel.onUpdateIsPlaying(isPlayingOnReady)
                if (!isPlaying)
                    viewModel.saveMediaIemCurrentPosition()
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
        mediaController.addListener(listener)
        onDispose {
            mediaController.removeListener(listener)
        }
    }

    if (isOverlayWindow) {
        OverlayVideoPlayerScreen(
            textureView = textureView,
            videoInfo = currentPlayingVideoInfo,
            isPlaying = isPlaying,
            mediaController = mediaController,
            modifier = modifier,
            windowLayoutState = windowLayoutState,
            attachToDraggable = attachDragBehavior,
            updateWindowSize = updateWindowSize
        )
    } else {
        DisposableEffect(Unit) {
            onDispose {
                mediaController.clearVideoTextureView(textureView)
            }
        }

        val isBackgroundPlayEnabled by viewModel.isBackgroundVideoPlayModeEnabled.collectAsState()
        val isInPipMode = rememberIsInPipMode()

        if (!isBackgroundPlayEnabled && isInPipMode) {
            PipPlayerScreen(
                textureView = textureView,
                mediaController = mediaController,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                isAudioOnly = isAudioOnly,
                viewModel = viewModel,
                modifier = modifier
            )

        } else {
            LargeVideoPlayerScreen(
                textureView = textureView,
                volumeKeyChannel = volumeKeyChannel,
                exoPlayer = mediaController,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                onPopUp = onPopUp,
                viewModel = viewModel,
                modifier = modifier,
                onUpdateSubtitleRef = { subtitleView ->
                    subtitleViewRef.set(subtitleView)
                }
            )
        }
    }
}