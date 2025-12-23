package com.flix.videos.ui.app.player.service.player

import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.flix.videos.ui.app.player.service.CMD_START_AUDIO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.CMD_START_VIDEO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.CMD_STOP_OVERLAY_VIDEO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.player.managers.ServiceMediaControllerManager
import com.flix.videos.ui.app.player.service.player.managers.notification.CustomMediaPlayerNotificationProvider
import com.flix.videos.ui.app.player.service.player.managers.notification.MediaPlayerNotificationManager
import com.flix.videos.ui.app.player.viewmodel.VideoParams
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject

@OptIn(UnstableApi::class)
abstract class MediaPlayerService : MediaSessionService() {
    val serviceMediaControllerManager: ServiceMediaControllerManager by inject()

    protected val videoParams = MutableStateFlow<VideoParams?>(null)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val mediaPlayerNotificationManager by inject<MediaPlayerNotificationManager>()

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var isBackgroundAudioMode = false

    val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
            onAudioSessionId(audioSessionId)
            setGainMillibels(1500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
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

        mediaSession = MediaSession.Builder(this, player)
            .setId("${packageName}:overlay_media_session")
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands =
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                            .add(SessionCommand(CMD_START_VIDEO_PLAYBACK_MODE, Bundle.EMPTY))
                            .add(SessionCommand(CMD_START_AUDIO_PLAYBACK_MODE, Bundle.EMPTY))
                            .add(SessionCommand(CMD_STOP_OVERLAY_VIDEO_PLAYBACK_MODE, Bundle.EMPTY))
                            .build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        CMD_START_VIDEO_PLAYBACK_MODE -> {
                            val group = args.getString("group")
                            val videoId = args.getLong("video_id")
                            videoParams.value = VideoParams(group, videoId)
                            return Futures.immediateFuture(
                                SessionResult(SessionResult.RESULT_SUCCESS)
                            )
                        }

                        CMD_START_AUDIO_PLAYBACK_MODE -> {
                            startForegroundMediaPlayer(session)
                            isBackgroundAudioMode = true
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }

                        CMD_STOP_OVERLAY_VIDEO_PLAYBACK_MODE -> {
                            serviceMediaControllerManager.releaseMediaController()
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()

                            return Futures.immediateFuture(
                                SessionResult(SessionResult.RESULT_SUCCESS)
                            )
                        }

                        else -> {
                            return super.onCustomCommand(session, controller, customCommand, args)
                        }
                    }
                }
            })
            .build()
        player.addListener(playerListener)
        setMediaNotificationProvider(CustomMediaPlayerNotificationProvider(this))
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

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean
    ) {
        if (isBackgroundAudioMode)
            startForegroundMediaPlayer(session)
        else
            super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY_PAUSE" -> mediaSession?.player?.apply {
                Log.e("AudioPlayerService", "isPlaying: $isPlaying, playWhenReady: $playWhenReady")
                if (isPlaying) pause() else {
                    if (playbackState == Player.STATE_ENDED) {
                        player.seekTo(0)
                        player.play()
                    } else {
                        player.play()
                    }
                }
            }

            "ACTION_NEXT" -> mediaSession?.player?.seekToNext()

            "ACTION_PREVIOUS" -> mediaSession?.player?.seekToPrevious()

            "ACTION_STOP" -> {
                mediaSession?.player?.stop()
                mediaSession?.player?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.e("Player", "onDestroy: Media player service")
        if (isBackgroundAudioMode) {
            stopForegroundMediaPlayer()
        }
        isBackgroundAudioMode = false
        serviceMediaControllerManager.releaseMediaController()
        releaseLoudnessEnhancer()
        mediaSession?.release()
        mediaSession = null
        player.removeListener(playerListener)
        player.release()
        super.onDestroy()
    }

    fun startForegroundMediaPlayer(mediaSession: MediaSession) {
        mediaPlayerNotificationManager.createMediaPlayBackNotificationChannel()
        val mediaPlayerNotification = mediaPlayerNotificationManager.getMediaPlayerNotification(
            mediaSession
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(
                MediaPlayerNotificationManager.NOTIFICATION_ID,
                mediaPlayerNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        else
            startForeground(MediaPlayerNotificationManager.NOTIFICATION_ID, mediaPlayerNotification)
    }

    fun stopForegroundMediaPlayer() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}