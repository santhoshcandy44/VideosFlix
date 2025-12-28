package com.flix.videos.ui.app.player.service.player

import android.app.Notification
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
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
import androidx.media3.ui.PlayerNotificationManager
import com.flix.videos.MainActivity
import com.flix.videos.R
import com.flix.videos.ui.app.player.service.CMD_START_AUDIO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.CMD_START_VIDEO_PLAYBACK_MODE
import com.flix.videos.ui.app.player.service.player.managers.ServiceMediaControllerManager
import com.flix.videos.ui.app.player.service.player.managers.notification.CustomMediaPlayerNotificationProvider
import com.flix.videos.ui.app.player.service.player.managers.notification.MediaPlayerNotificationManager
import com.flix.videos.ui.app.player.viewmodel.VideoParams
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

@OptIn(UnstableApi::class)
abstract class MediaPlayerService : MediaSessionService() {
    val serviceMediaControllerManager: ServiceMediaControllerManager by inject()
    private val mediaPlayerNotificationManager by inject<MediaPlayerNotificationManager>()

    protected val videoParams = MutableStateFlow<VideoParams?>(null)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null


    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var isBackgroundAudioMode = false
    private var isBackgroundVideoMode = false

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
            onAudioSessionId(audioSessionId)
            setGainMillibels(1500)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_ENDED) {
                player.stop()
                player.playWhenReady = false
                player.seekTo(0, 0)
                player.prepare()
            }
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
                            isBackgroundVideoMode = true
                            val group = args.getString("group")
                            val videoId = args.getLong("video_id")
                            videoParams.value = VideoParams(group, videoId)
                            return Futures.immediateFuture(
                                SessionResult(SessionResult.RESULT_SUCCESS)
                            )
                        }

                        CMD_START_AUDIO_PLAYBACK_MODE -> {
                            startForegroundMediaPlayer()
                            isBackgroundAudioMode = true
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }

                        else -> {
                            return super.onCustomCommand(session, controller, customCommand, args)
                        }
                    }
                }

                override fun onDisconnected(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ) {
                    if (isBackgroundVideoMode) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    super.onDisconnected(session, controller)
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
        if (isBackgroundVideoMode)
            super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    var playerNotificationManager: PlayerNotificationManager? = null

    override fun onDestroy() {
        isBackgroundAudioMode = false
        isBackgroundVideoMode = false
        playerNotificationManager?.setPlayer(null)
        serviceMediaControllerManager.releaseMediaController()
        releaseLoudnessEnhancer()
        mediaSession?.release()
        mediaSession = null
        player.removeListener(playerListener)
        player.release()
        super.onDestroy()
    }

    fun startForegroundMediaPlayer() {
        mediaPlayerNotificationManager.createMediaPlayBackNotificationChannel()
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            MediaPlayerNotificationManager.NOTIFICATION_ID,
            MediaPlayerNotificationManager.CHANNEL_ID
        )
            .setMediaDescriptionAdapter(descriptionAdapter)
            .setNotificationListener(notificationListener)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_video_play)
                setColorized(false)
                setUseRewindAction(false)
                setUseFastForwardAction(false)
                setUsePreviousActionInCompactView(true)
                setUseNextActionInCompactView(true)
                setUseChronometer(true)
                setUseStopAction(true)
            }
        playerNotificationManager!!.setPlayer(player)
    }

    private val descriptionAdapter =
        object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.mediaMetadata.title ?: ""
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return player.mediaMetadata.artist
            }

            @Suppress("DEPRECATION")
            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? {
                val mediaUri = player.currentMediaItem?.localConfiguration?.uri
                    ?: return null
                CoroutineScope(Dispatchers.IO).launch {
                    val bitmap = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentResolver.loadThumbnail(
                                mediaUri,
                                Size(300, 300),
                                null
                            )
                        } else {
                            val id = ContentUris.parseId(mediaUri)
                            MediaStore.Video.Thumbnails.getThumbnail(
                                contentResolver,
                                id,
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null
                            )
                        }
                    } catch (_: Exception) {
                        null
                    }

                    withContext(Dispatchers.Main) {
                        bitmap?.let { callback.onBitmap(it) }
                    }
                }
                return null
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                return PendingIntent.getActivity(
                    this@MediaPlayerService,
                    0,
                    Intent(this@MediaPlayerService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

    private val notificationListener =
        object : PlayerNotificationManager.NotificationListener {

            override fun onNotificationPosted(
                notificationId: Int,
                notification: Notification,
                ongoing: Boolean
            ) {
                if (ongoing)
                    startForeground(notificationId, notification)
            }

            override fun onNotificationCancelled(
                notificationId: Int,
                dismissedByUser: Boolean
            ) {
                serviceMediaControllerManager.releaseMediaController()
            }
        }
}