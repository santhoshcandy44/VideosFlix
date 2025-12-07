package com.flix.videos.ui.app.player.viewmodel

import android.app.PictureInPictureParams
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import androidx.core.net.toUri

object ExoplayerSeekDirection {
    const val SEEK_NONE = 0
    const val SEEK_FORWARD = 1
    const val SEEK_BACKWARD = -1
}

@OptIn(UnstableApi::class)
@KoinViewModel
class VideoPlayerViewModel
    (
    val applicationContext: Context,
    @InjectedParam uri: String,
    @InjectedParam val title: String,
    @InjectedParam val videoWidth: Int,
    @InjectedParam val videoHeight: Int,
    @InjectedParam val totalDurationMillis: Long
) : ViewModel() {
    val videoUri: Uri = uri.toUri()

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

    private var progressJob: Job? = null

    val pipBuilder = PictureInPictureParams.Builder()

    init {
        val mediaItem = MediaItem.fromUri(videoUri)
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(), true
        )
        exoPlayer.setMediaSource(
            DefaultMediaSourceFactory(
                DefaultDataSource.Factory(applicationContext),
            ).createMediaSource(mediaItem)
        )
        exoPlayer.prepare()
        exoPlayer.play()
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
        _currentDurationMillis.value = (value * totalDurationMillis).toLong()
    }

    fun onSliderValueChangeFinished() {
        val updatedCurrentDurationMillis = (_sliderProgress.value * totalDurationMillis).toLong()
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
        exoPlayer.volume = if(_isMuted.value) 0f else 1f
    }

    fun seekForward(millis: Long = 10_000) {
        val newPos = (exoPlayer.currentPosition + millis).coerceAtMost(totalDurationMillis)
        exoPlayer.seekTo(newPos)
    }

    fun seekBackward(millis: Long = 10_000) {
        val newPos = (exoPlayer.currentPosition - millis).coerceAtLeast(0)
        exoPlayer.seekTo(newPos)
    }

    fun onFastSeekFinished() {
        val sliderValue = (exoPlayer.currentPosition / totalDurationMillis.toFloat())
            .takeIf { totalDurationMillis > 0 }
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
                    _sliderProgress.value = pos / totalDurationMillis.toFloat()
                }
                delay(33)
            }
        }
    }

    fun stopUpdatingProgress() {
        progressJob?.cancel()
        progressJob = null
    }
}