package com.flix.videos.ui.app.player.viewmodel

import android.app.PictureInPictureParams
import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.player.ExoPlayerRepeatMode
import com.flix.videos.ui.app.player.prefs.AudioTrackPrefs
import com.flix.videos.ui.app.player.prefs.PlaybackPosPrefs
import com.flix.videos.ui.app.player.prefs.PlaybackSettingsPrefs
import com.flix.videos.ui.app.player.prefs.SubtitlePrefs
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
class VideoPlayerViewModel
    (
    val applicationContext: Context,
    @InjectedParam videoParams: VideoParams,
    val mediaSourceRepository: MediaSourceRepository,
    val plaBackPosPrefs: PlaybackPosPrefs,
    val subtitlePrefs: SubtitlePrefs,
    val playbackSettingsPrefs: PlaybackSettingsPrefs,
    val audioTrackPrefs: AudioTrackPrefs
) : ViewModel() {
    val exoPlayer = ExoPlayer.Builder(applicationContext.applicationContext).build()

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

    private val defaultMediaSourceFactory =
        DefaultMediaSourceFactory(DefaultDataSource.Factory(applicationContext))

    val subtitleObserver = SubtitleFilesObserver(applicationContext) {
        _localSubtitles.value = mediaSourceRepository.getSubtitleFiles()
    }

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

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

            val playItemIndex = findVideoIndexById(requiredVideos, videoParams.id)
            _currentPlayingVideoInfo.value = requiredVideos.getOrElse(playItemIndex,
                { VideoInfo.EMPTY })
            val mediaSources = requiredVideos.map { videoInfo ->
                defaultMediaSourceFactory.createMediaSource(
                    MediaItem.Builder()
                        .setUri(videoInfo.uri)
                        .setTag(videoInfo)
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
                )
            }
            exoPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(), true
            )
            exoPlayer.setMediaSources(
                mediaSources,
                playItemIndex,
                plaBackPosPrefs.getPosition(_currentPlayingVideoInfo.value.uri)
            )
            _currentLocalSubtitle.value =
                subtitlePrefs.getSavedSubtitleUri(_currentPlayingVideoInfo.value.uri)?.let {
                    mediaSourceRepository.getSubtitleInfoFromUri(it)
                }
            applyRepeatMode(_currentRepeatMode.value)
            if (_isSubtitleEnabled.value)
                enableSubtitles()
            else
                disableSubtitles()
            exoPlayer.setPlaybackSpeed(_currentPlayPackSpeed.value)
            exoPlayer.prepare()
            exoPlayer.play()

            _isLoading.value = false
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
        return videos.indexOfFirst { it.id == videoId }
            .takeIf { it != -1 } ?: 0
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
        exoPlayer.seekTo(updatedCurrentDurationMillis)
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    fun onUpdateCurrentDurationMillis(value: Long) {
        _currentDurationMillis.value = value
    }

    fun onUpdateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.seekTo(0)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                exoPlayer.play()
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        exoPlayer.volume = if (_isMuted.value) 0f else 1f
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        exoPlayer.volume = if (muted) 0f else 1f
    }

    fun updateLockedOrientation(isLocked: Boolean) {
        _isLockedOrientation.value = isLocked
    }

    fun toggleAudioOnly() {
        _isAudioOnly.value = !_isAudioOnly.value
        playbackSettingsPrefs.setAudioOnly(_isAudioOnly.value)
    }

    fun setCurrentPlayBackSpeed(speed: Float) {
        _currentPlayPackSpeed.value = speed
        playbackSettingsPrefs.setPlaybackSpeed(speed)
        exoPlayer.setPlaybackSpeed(speed)
    }

    fun setCurrentPlayListRepeatMode(mode: ExoPlayerRepeatMode) {
        _currentRepeatMode.value = mode
        playbackSettingsPrefs.setPlaybackMode(mode)
        applyRepeatMode(mode)
    }

    private fun applyRepeatMode(mode: ExoPlayerRepeatMode) {
        return when (mode) {
            ExoPlayerRepeatMode.REPEAT_MODE_OFF -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.shuffleModeEnabled = false
            }

            ExoPlayerRepeatMode.REPEAT_MODE_ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.shuffleModeEnabled = false
            }

            ExoPlayerRepeatMode.REPEAT_MODE_ALL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                exoPlayer.shuffleModeEnabled = false
            }

            ExoPlayerRepeatMode.SHUFFLE -> {
                exoPlayer.shuffleModeEnabled = true
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
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
        val groups = exoPlayer.currentTracks.groups
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
        return null
    }

    private fun getCurrentSubtitleTrack(): SubtitleTrackInfo? {
        val groups = exoPlayer.currentTracks.groups
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
        return null
    }

    fun switchAudioTrack(
        groupIndex: Int,
        trackIndex: Int
    ) {
        val group = exoPlayer.currentTracks.groups[groupIndex]

        val override = TrackSelectionOverride(
            group.mediaTrackGroup,
            listOf(trackIndex)
        )

        val newParams = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setOverrideForType(override)
            .build()

        exoPlayer.trackSelectionParameters = newParams
        audioTrackPrefs.saveAudioTrack(_currentPlayingVideoInfo.value.uri, groupIndex, trackIndex)
    }

    fun switchSubTitleTrack(
        subtitleTrack: SubtitleTrackInfo
    ) {
        val groupIndex = subtitleTrack.groupIndex
        val trackIndex = subtitleTrack.trackIndex

        val group = exoPlayer.currentTracks.groups[groupIndex]

        val override = TrackSelectionOverride(
            group.mediaTrackGroup,
            listOf(trackIndex)
        )

        val newParams = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setOverrideForType(override)
            .build()

        exoPlayer.trackSelectionParameters = newParams
        subtitlePrefs.clearSubtitle(_currentPlayingVideoInfo.value.uri)
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
        val params = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()

        exoPlayer.trackSelectionParameters = params
    }

    fun disableSubtitles() {
        val params = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()

        exoPlayer.trackSelectionParameters = params
    }

    fun updateCurrentLocalSubtitle(subtitleFileInfo: SubtitleFileInfo) {
        _currentLocalSubtitle.value = subtitleFileInfo
        applyLocalSubtitle(subtitleFileInfo.uri)
        subtitlePrefs.saveSubtitleUri(_currentPlayingVideoInfo.value.uri, subtitleFileInfo.uri)
    }

    private fun applyLocalSubtitle(uri: Uri) {
        val currentIndex = exoPlayer.currentMediaItemIndex
        val position = exoPlayer.currentPosition
        val playWhenReady = exoPlayer.playWhenReady

        // Create subtitle config
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(
                mediaSourceRepository.detectSubtitleMimeType(uri)
            )  // auto-detect (SRT/ASS/VTT)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .setRoleFlags(C.ROLE_FLAG_CAPTION)
            .setLanguage("und")
            .build()

        val newSources = requiredVideos.map { video ->
            val item = MediaItem.Builder()
                .setUri(video.uri)
                .setTag(video)
                .apply {
                    if (video.id == _currentPlayingVideoInfo.value.id) {
                        setSubtitleConfigurations(listOf(subtitleConfig))
                    }
                }
                .build()
            defaultMediaSourceFactory.createMediaSource(item)
        }

        exoPlayer.setMediaSources(
            newSources,
            currentIndex,
            position
        )
        exoPlayer.prepare()
        exoPlayer.playWhenReady = playWhenReady

        val params = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()

        exoPlayer.trackSelectionParameters = params
    }

    fun seekForward(millis: Long = 10_000) {
        val newPos =
            (exoPlayer.currentPosition + millis).coerceAtMost(_currentPlayingVideoInfo.value.duration)
        exoPlayer.seekTo(newPos)
    }

    fun seekBackward(millis: Long = 10_000) {
        val newPos = (exoPlayer.currentPosition - millis).coerceAtLeast(0)
        exoPlayer.seekTo(newPos)
    }

    fun seekToNext() {
        plaBackPosPrefs.savePosition(_currentPlayingVideoInfo.value.uri, exoPlayer.currentPosition)
        exoPlayer.seekToNextMediaItem()
    }

    fun seekToPrevious() {
        plaBackPosPrefs.savePosition(_currentPlayingVideoInfo.value.uri, exoPlayer.currentPosition)
        exoPlayer.seekToPreviousMediaItem()
    }

    fun onFastSeekFinished() {
        val sliderValue =
            (exoPlayer.currentPosition / _currentPlayingVideoInfo.value.duration.toFloat())
                .takeIf { _currentPlayingVideoInfo.value.duration > 0 }
                ?.coerceIn(0f, 1f)
                ?: 0f
        onSliderValueChange(sliderValue)
    }

    fun startUpdatingProgress() {
        if (progressJob?.isActive == true) return
        progressJob = viewModelScope.launch {
            while (isActive) {
                if (!isSliderValueChange) {
                    val pos = exoPlayer.currentPosition
                    _currentDurationMillis.value = pos
                    _sliderProgress.value = pos / _currentPlayingVideoInfo.value.duration.toFloat()
                }
                delay(33)
            }
        }
    }

    fun stopUpdatingProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    fun saveMediaIemCurrentPosition() {
        val configuration = exoPlayer.currentMediaItem?.localConfiguration
        configuration?.let {
            plaBackPosPrefs.savePosition(it.uri, exoPlayer.currentPosition)
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

    fun onAudioSessionId(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        loudnessEnhancer?.release()
        loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
            enabled = true
        }
    }

    fun setGainMillibels(mb: Int) {
        loudnessEnhancer?.setTargetGain(mb.coerceIn(0, 1500))
    }

    fun releaseLoudnessEnhancer() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }

    override fun onCleared() {
        super.onCleared()
        releaseLoudnessEnhancer()
        applicationContext.contentResolver.unregisterContentObserver(subtitleObserver)
    }
}