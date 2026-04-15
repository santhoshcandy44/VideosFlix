package com.flix.videos.ui.app.player.viewmodel

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.ExoPlayerRepeatMode
import com.flix.videos.ui.app.player.enterFullScreenMode
import com.flix.videos.ui.app.player.prefs.AudioTrackPrefs
import com.flix.videos.ui.app.player.prefs.PlaybackPosPrefs
import com.flix.videos.ui.app.player.prefs.PlaybackSettingsPrefs
import com.flix.videos.ui.app.player.prefs.SubtitlePrefs
import com.flix.videos.ui.app.player.service.player.managers.ControllerSource
import com.flix.videos.ui.app.player.service.player.managers.MediaControllerManager
import com.flix.videos.ui.app.player.service.player.managers.ServiceMediaControllerManager
import com.flix.videos.ui.app.viewmodel.MediaSourceRepository
import com.flix.videos.ui.app.viewmodel.SubtitleFileInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.io.File

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val language: String?,
    val label: String?
)

data class SubtitleTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val language: String?,
    val label: String?
)

data class VideoParams(
    val group: String?,
    val id: Long
)

@OptIn(UnstableApi::class)
@KoinViewModel
class VideoPlayerViewModel(
    val applicationContext: Context,
    @InjectedParam val isWindowMini: Boolean,
    @InjectedParam val videoParams: VideoParams,
    @InjectedParam val mediaControllerManager: MediaControllerManager?,
    @InjectedParam val serviceMediaControllerManager: ServiceMediaControllerManager?,
    val mediaSourceRepository: MediaSourceRepository,
    val plaBackPosPrefs: PlaybackPosPrefs,
    val subtitlePrefs: SubtitlePrefs,
    val playbackSettingsPrefs: PlaybackSettingsPrefs,
    val audioTrackPrefs: AudioTrackPrefs
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _controllerState = MutableStateFlow<MediaController?>(null)
    val controllerState = _controllerState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentDurationMillis = MutableStateFlow(0L)
    val currentDurationMillis = _currentDurationMillis.asStateFlow()

    var isSliderValueChange = false

    private val _sliderProgress = MutableStateFlow(0f)
    val sliderProgress = _sliderProgress.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(false)
    val isControlsVisible = _isControlsVisible.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _isLockedOrientation = MutableStateFlow(false)
    val isLockedOrientation = _isLockedOrientation.asStateFlow()

    private var progressJob: Job? = null

    val pipBuilder = PictureInPictureParams.Builder()

    private val _isAudioOnly = MutableStateFlow(playbackSettingsPrefs.isAudioOnly())
    val isAudioOnly = _isAudioOnly.asStateFlow()

    private val _isBackgroundVideoPlayModeEnabled =
        MutableStateFlow(playbackSettingsPrefs.isBackgroundVideoPlayModeEnabled())
    val isBackgroundVideoPlayModeEnabled = _isBackgroundVideoPlayModeEnabled.asStateFlow()

    val playBackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    private val _currentPlayPackSpeed = MutableStateFlow(playbackSettingsPrefs.getPlaybackSpeed())
    val currentPlayPackSpeed = _currentPlayPackSpeed.asStateFlow()

    private val _currentRepeatMode = MutableStateFlow(playbackSettingsPrefs.getPlaybackMode())
    val currentRepeatMode = _currentRepeatMode.asStateFlow()

    var allVideos = emptyList<VideoInfo>()

    var groupedVideos = emptyMap<String, List<VideoInfo>>()

    var requiredVideos = emptyList<VideoInfo>()

    private val _currentPlayingVideoInfo = MutableStateFlow(
        VideoInfo.EMPTY
    )
    val currentPlayingVideoInfo = _currentPlayingVideoInfo.asStateFlow()

    private val _currentAudioTrack = MutableStateFlow<AudioTrackInfo?>(null)
    val currentAudioTrack = _currentAudioTrack.asStateFlow()

    private val _localSubtitles = MutableStateFlow<List<SubtitleFileInfo>>(emptyList())
    val localSubtitles = _localSubtitles.asStateFlow()

    private val _isSubtitleEnabled = MutableStateFlow(playbackSettingsPrefs.isSubtitlesEnabled())
    val isSubtitleEnabled = _isSubtitleEnabled.asStateFlow()

    private val _currentLocalSubtitle = MutableStateFlow<SubtitleFileInfo?>(null)
    val currentLocalSubtitle = _currentLocalSubtitle.asStateFlow()

    private val _currentSubtitleTrack = MutableStateFlow<SubtitleTrackInfo?>(null)
    val currentSubtitleTrack = _currentSubtitleTrack.asStateFlow()

    //Player Controls Hide Job
    private var playerControlsHideJob: Job? = null

    fun createControlsHideJob(activity: Activity, timeMillis: Long = 5000) {
        playerControlsHideJob = viewModelScope.launch {
            delay(timeMillis)
            enterFullScreenMode(activity)
            hideControls()
        }
    }

    fun createControlsHideJobIfNot(activity: Activity, timeMillis: Long = 5000) {
        if (playerControlsHideJob != null) return
        createControlsHideJob(activity, timeMillis)
    }

    fun cancelControlsHideJob() {
        playerControlsHideJob?.cancel()
        playerControlsHideJob = null
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private val subtitleObserver = SubtitleFilesObserver(applicationContext) {
        _localSubtitles.value = mediaSourceRepository.getSubtitleFiles()
    }

    init {
        viewModelScope.launch {
            allVideos = mediaSourceRepository.getAllVideos()
            groupedVideos = allVideos.groupBy { File(it.path).parent ?: "Unknown" }
                .mapValues { (groupParent, list) ->
                    list.map { video ->
                        val groupParentFile = File(groupParent)
                        val absPath = groupParentFile.absolutePath
                        val root = Environment.getExternalStorageDirectory().absolutePath
                        video.copy(
                            displayGroupName = if (absPath == root) "Root" else groupParentFile.name
                        )
                    }
                }
            requiredVideos = if (videoParams.group != null) groupedVideos[videoParams.group]
                ?: emptyList() else allVideos

            val controllerManager =
                if (isWindowMini) serviceMediaControllerManager!! else mediaControllerManager!!

            val sessionToken = if (isWindowMini)
                null
            else {
                player = ExoPlayer.Builder(applicationContext).build().apply {
                    setHandleAudioBecomingNoisy(true)
                    setPriority(C.PRIORITY_PLAYBACK)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        true
                    )
                }

                mediaSession = MediaSession.Builder(applicationContext, player)
                    .setId("local_media_session")
                    .build()

                mediaSession.token
            }

            controllerManager.getController(
                sessionToken,
                Bundle.EMPTY
            ) { mediaController, source ->
                _controllerState.value = mediaController
                _currentLocalSubtitle.value =
                    subtitlePrefs.getSavedSubtitleUri(_currentPlayingVideoInfo.value.uri)
                        ?.let { mediaSourceRepository.getSubtitleInfoFromUri(it) }

                applyRepeatMode(_currentRepeatMode.value)
                if (_isSubtitleEnabled.value)
                    enableSubtitles()
                else
                    disableSubtitles()
                mediaController.setPlaybackSpeed(_currentPlayPackSpeed.value)

                val playItemIndex = findVideoIndexById(
                    requiredVideos,
                    videoParams.id
                ).let { if (it == -1) 0 else it }
                _currentPlayingVideoInfo.value = requiredVideos.getOrElse(
                    playItemIndex
                ) { VideoInfo.EMPTY }

                val mediaItems = requiredVideos.map { videoInfo ->
                    MediaItem.Builder()
                        .setUri(videoInfo.uri)
                        .setMediaId(videoInfo.id.toString())
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(videoInfo.title)
                                .build()
                        )
                        .apply {
                            subtitlePrefs.getSavedSubtitleUri(videoInfo.uri)?.let {
                                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(it)
                                    .setMimeType(mediaSourceRepository.detectSubtitleMimeType(it))  // auto-detect (SRT/ASS/VTT)
                                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                    .setRoleFlags(C.ROLE_FLAG_CAPTION)
                                    .setLanguage("und")
                                    .build()
                                setSubtitleConfigurations(listOf(subtitleConfig))
                            }
                        }
                        .build()
                }

                mediaController.setMediaItems(
                    mediaItems,
                    playItemIndex,
                    plaBackPosPrefs.getPosition(_currentPlayingVideoInfo.value.uri)
                )

                if (source == ControllerSource.ACTIVITY) {
                    mediaController.prepare()
                    mediaController.play()
                }

                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            _localSubtitles.value = mediaSourceRepository.getSubtitleFiles()
            applicationContext.contentResolver.registerContentObserver(
                MediaStore.Files.getContentUri("external"),
                true,
                subtitleObserver
            )
        }
    }

    fun findVideoIndexById(videos: List<VideoInfo>, videoId: Long): Int {
        return videos.indexOfFirst { it.id == videoId }.takeIf { it != -1 } ?: -1
    }

    fun getCurrentPlayingVideoInfo(mediaId: Long): VideoInfo? {
        return requiredVideos.getOrNull(findVideoIndexById(requiredVideos, mediaId))
    }

    fun setCurrentPlayingVideoInfo(videoInfo: VideoInfo) {
        _currentPlayingVideoInfo.value = videoInfo
    }

    fun showControls() {
        _isControlsVisible.value = true
    }

    fun hideControls() {
        _isControlsVisible.value = false
    }

    fun onUpdateSliderValueChange(isValueChange: Boolean) {
        isSliderValueChange = isValueChange
    }

    fun onSliderValueChange(value: Float) {
        _sliderProgress.value = value
        _currentDurationMillis.value = (value * _currentPlayingVideoInfo.value.duration).toLong()
    }

    fun onSliderValueChangeFinished() {
        val updatedCurrentDurationMillis =
            (_sliderProgress.value * _currentPlayingVideoInfo.value.duration).toLong()
        onUpdateCurrentDurationMillis(updatedCurrentDurationMillis)
        _controllerState.value?.let { controller ->
            controller.seekTo(updatedCurrentDurationMillis)
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
                controller.playWhenReady = true
            }
        }
    }

    fun onUpdateCurrentDurationMillis(value: Long) {
        _currentDurationMillis.value = value
    }

    fun onUpdateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun togglePlayPause() {
        _controllerState.value?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_IDLE) {
                    controller.seekTo(0)
                    controller.prepare()
                    controller.playWhenReady = true
                } else {
                    controller.play()
                }
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        _controllerState.value?.volume = if (_isMuted.value) 0f else 1f
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        _controllerState.value?.volume = if (muted) 0f else 1f
    }

    fun updateLockedOrientation(isLocked: Boolean) {
        _isLockedOrientation.value = isLocked
    }

    fun toggleAudioOnly() {
        _isAudioOnly.value = !_isAudioOnly.value
        playbackSettingsPrefs.setAudioOnly(_isAudioOnly.value)
    }

    fun toggleBackgroundVideoPlayModeEnabled(isChecked: Boolean) {
        _isBackgroundVideoPlayModeEnabled.value = isChecked
        playbackSettingsPrefs.setBackgroundVideoPlayModeEnabled(isChecked)
    }

    fun setCurrentPlayBackSpeed(speed: Float) {
        _currentPlayPackSpeed.value = speed
        playbackSettingsPrefs.setPlaybackSpeed(speed)
        _controllerState.value?.setPlaybackSpeed(speed)
    }

    fun setCurrentPlayListRepeatMode(mode: ExoPlayerRepeatMode) {
        _currentRepeatMode.value = mode
        playbackSettingsPrefs.setPlaybackMode(mode)
        applyRepeatMode(mode)
    }

    private fun applyRepeatMode(mode: ExoPlayerRepeatMode) {
        _controllerState.value?.let { controller ->
            when (mode) {
                ExoPlayerRepeatMode.REPEAT_MODE_OFF -> {
                    controller.repeatMode = Player.REPEAT_MODE_OFF
                    controller.shuffleModeEnabled = false
                }

                ExoPlayerRepeatMode.REPEAT_MODE_ONE -> {
                    controller.repeatMode = Player.REPEAT_MODE_ONE
                    controller.shuffleModeEnabled = false
                }

                ExoPlayerRepeatMode.REPEAT_MODE_ALL -> {
                    controller.repeatMode = Player.REPEAT_MODE_ALL
                    controller.shuffleModeEnabled = false
                }

                ExoPlayerRepeatMode.SHUFFLE -> {
                    controller.shuffleModeEnabled = true
                    controller.repeatMode = Player.REPEAT_MODE_ALL
                }
            }
        }
    }

    fun setCurrentAudioTrack() {
        _currentAudioTrack.value = getCurrentAudioTrack()
    }

    fun setCurrentSubtitleTrack() {
        _currentSubtitleTrack.value = getCurrentSubtitleTrack()
    }

    private fun getCurrentAudioTrack(): AudioTrackInfo? {
        _controllerState.value?.let { controller ->
            val groups = controller.currentTracks.groups
            groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (trackIndex in 0 until group.length) {
                        if (group.isTrackSelected(trackIndex)) {
                            val format = group.mediaTrackGroup.getFormat(trackIndex)

                            val displayLabel =
                                if (format.language != null && format.label != null) {
                                    "${format.language}_${format.label}"
                                } else {
                                    "Audio Track ${trackIndex + 1}"
                                }

                            return AudioTrackInfo(
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                language = format.language,
                                label = displayLabel
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    private fun getCurrentSubtitleTrack(): SubtitleTrackInfo? {
        _controllerState.value?.let { controller ->
            val groups = controller.currentTracks.groups
            groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (trackIndex in 0 until group.length) {
                        if (group.isTrackSelected(trackIndex)) {
                            val format = group.mediaTrackGroup.getFormat(trackIndex)
                            val displayLabel =
                                if (format.language != null && format.label != null) {
                                    "${format.language}_${format.label}"
                                } else {
                                    "Subtitle ${trackIndex + 1}"
                                }
                            return SubtitleTrackInfo(
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                language = format.language,
                                label = displayLabel
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    fun switchAudioTrack(
        groupIndex: Int,
        trackIndex: Int
    ): Boolean {
        _controllerState.value?.let { controller ->
            val group = controller.currentTracks.groups.getOrNull(groupIndex) ?: return false

            val override = TrackSelectionOverride(
                group.mediaTrackGroup,
                listOf(trackIndex)
            )

            val newParams = controller.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setOverrideForType(override)
                .build()

            controller.trackSelectionParameters = newParams
            audioTrackPrefs.saveAudioTrack(
                _currentPlayingVideoInfo.value.uri,
                groupIndex,
                trackIndex
            )

            return true
        }
        return false
    }

    fun switchSubTitleTrack(
        subtitleTrack: SubtitleTrackInfo
    ): Boolean {
        _controllerState.value?.let { controller ->
            val groupIndex = subtitleTrack.groupIndex
            val trackIndex = subtitleTrack.trackIndex

            val group = controller.currentTracks.groups.getOrNull(groupIndex) ?: return false

            val override = TrackSelectionOverride(
                group.mediaTrackGroup,
                listOf(trackIndex)
            )

            val newParams = controller.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setOverrideForType(override)
                .build()

            controller.trackSelectionParameters = newParams
            subtitlePrefs.clearSubtitle(_currentPlayingVideoInfo.value.uri)
            return true
        }

        return false
    }

    fun onSubtitleToggle(isChecked: Boolean) {
        _isSubtitleEnabled.value = isChecked
        if (isChecked)
            enableSubtitles()
        else
            disableSubtitles()
        playbackSettingsPrefs.setSubtitlesEnabled(isChecked)
    }

    fun enableSubtitles() {
        _controllerState.value?.let { controller ->
            val params = controller.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()

            controller.trackSelectionParameters = params
        }
    }

    fun disableSubtitles() {
        _controllerState.value?.let { controller ->
            val params = controller.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()

            controller.trackSelectionParameters = params
        }
    }

    fun updateCurrentLocalSubtitle(subtitleFileInfo: SubtitleFileInfo) {
        _currentLocalSubtitle.value = subtitleFileInfo
        applyLocalSubtitle(subtitleFileInfo.uri)
        subtitlePrefs.saveSubtitleUri(_currentPlayingVideoInfo.value.uri, subtitleFileInfo.uri)
    }

    private fun applyLocalSubtitle(uri: Uri) {
        _controllerState.value?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val position = controller.currentPosition
            val playWhenReady = controller.playWhenReady

            // Create subtitle config
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(
                    mediaSourceRepository.detectSubtitleMimeType(uri)
                )  // auto-detect (SRT/ASS/VTT)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setRoleFlags(C.ROLE_FLAG_CAPTION)
                .setLanguage("und")
                .build()

            val newMediaItems = requiredVideos.map { video ->
                MediaItem.Builder()
                    .setUri(video.uri)
                    .setTag(video)
                    .apply {
                        if (video.id == _currentPlayingVideoInfo.value.id) {
                            setSubtitleConfigurations(listOf(subtitleConfig))
                        }
                    }
                    .build()
            }

            controller.setMediaItems(
                newMediaItems,
                currentIndex,
                position
            )
            controller.prepare()
            controller.playWhenReady = playWhenReady

            val params = controller.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()

            controller.trackSelectionParameters = params
        }
    }

    fun seekForward(millis: Long = 10_000) {
        _controllerState.value?.let { controller ->
            val newPos =
                (controller.currentPosition + millis).coerceAtMost(_currentPlayingVideoInfo.value.duration)
            controller.seekTo(newPos)
        }
    }

    fun seekBackward(millis: Long = 10_000) {
        _controllerState.value?.let { controller ->
            val newPos = (controller.currentPosition - millis).coerceAtLeast(0)
            controller.seekTo(newPos)
        }
    }

    fun seekToNext() {
        _controllerState.value?.let { controller ->
            plaBackPosPrefs.savePosition(
                _currentPlayingVideoInfo.value.uri,
                controller.currentPosition
            )
            controller.seekToNextMediaItem()
        }
    }

    fun seekToPrevious() {
        _controllerState.value?.let { controller ->
            plaBackPosPrefs.savePosition(
                _currentPlayingVideoInfo.value.uri,
                controller.currentPosition
            )
            controller.seekToPreviousMediaItem()
        }
    }

    fun onFastSeekFinished() {
        _controllerState.value?.let { controller ->
            val sliderValue =
                (controller.currentPosition / _currentPlayingVideoInfo.value.duration.toFloat())
                    .takeIf { _currentPlayingVideoInfo.value.duration > 0 }
                    ?.coerceIn(0f, 1f)
                    ?: 0f
            onSliderValueChange(sliderValue)
        }
    }

    fun startUpdatingProgress() {
        if (progressJob?.isActive == true) return
        _controllerState.value?.let { controller ->
            progressJob = viewModelScope.launch {
                while (isActive) {
                    if (!isSliderValueChange) {
                        val pos = controller.currentPosition
                        _currentDurationMillis.value = pos
                        _sliderProgress.value =
                            pos / _currentPlayingVideoInfo.value.duration.toFloat()
                    }
                    delay(33)
                }
            }
        }
    }

    fun stopUpdatingProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    fun saveMediaIemCurrentPosition() {
        _controllerState.value?.let { controller ->
            val configuration = controller.currentMediaItem?.localConfiguration
            configuration?.let {
                plaBackPosPrefs.savePosition(it.uri, controller.currentPosition)
            }
        }
    }

    fun getMediaIemLastPosition(uri: Uri): Long {
        return plaBackPosPrefs.getPosition(uri)
    }

    fun clearMediaItemPosition(uri: Uri) {
        plaBackPosPrefs.clearPosition(uri)
    }

    fun getMediaIemAudioTrack(uri: Uri): Pair<Int, Int>? {
        return audioTrackPrefs.getSavedAudioTrack(uri)
    }

    fun releaseMediaController() {
        mediaControllerManager?.releaseMediaController()
        mediaSession.release()
        player.release()
    }

    override fun onCleared() {
        super.onCleared()
        applicationContext.contentResolver.unregisterContentObserver(subtitleObserver)
        if (!isWindowMini)
            releaseMediaController()
    }
}