package com.flix.videos.ui.app.player.service.player.managers.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.flix.videos.MainActivity
import com.flix.videos.R
import com.flix.videos.ui.app.player.PlayerActivity
import com.flix.videos.ui.app.player.service.player.MediaPlayerService
import org.koin.core.annotation.Factory

@Factory
class MediaPlayerNotificationManager(val applicationContext: Context) {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "media_playback"
    }

    fun getMediaPlayerNotification(mediaSession: MediaSession): Notification {
        return NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setContentTitle(mediaSession.player.mediaMetadata.title ?: "Playing")
            .setContentText(null)
            .setSmallIcon(R.drawable.ic_video_play)
            .addAction(
                R.drawable.ic_video_backward,
                "Previous",
                createPendingIntent(Player.COMMAND_SEEK_TO_PREVIOUS)
            )
            .addAction(
                if (mediaSession.player.isPlaying) {
                    NotificationCompat.Action(
                        R.drawable.ic_video_pause,
                        "Play",
                        createPendingIntent(Player.COMMAND_PLAY_PAUSE)
                    )
                } else {
                    NotificationCompat.Action(
                        R.drawable.ic_video_play,
                        "Pause",
                        createPendingIntent(Player.COMMAND_PLAY_PAUSE)
                    )
                }
            )
            .addAction(
                R.drawable.ic_video_forward,
                "Next",
                createPendingIntent(Player.COMMAND_SEEK_TO_NEXT)
            )
            .setStyle(
               androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setContentIntent(createContentIntent())
            .setOngoing(true)
            .setDeleteIntent(createDeleteIntent())
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createPendingIntent(command: Int): PendingIntent {
        val intent = Intent(applicationContext, MediaPlayerService::class.java).apply {
            action = when (command) {
                Player.COMMAND_PLAY_PAUSE -> "ACTION_PLAY_PAUSE"
                Player.COMMAND_SEEK_TO_NEXT -> "ACTION_NEXT"
                Player.COMMAND_SEEK_TO_PREVIOUS -> "ACTION_PREVIOUS"
                else -> "ACTION_PLAY_PAUSE"
            }
        }
        return PendingIntent.getService(
            applicationContext,
            command,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "ACTION_NOTIFICATION_MEDIA_PLAYBACK"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createDeleteIntent(): PendingIntent {
        val intent = Intent(applicationContext, MediaPlayerService::class.java).apply {
            action = "ACTION_STOP"
        }
        return PendingIntent.getService(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createMediaPlayBackNotificationChannel() {
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    setShowBadge(false)
                }
            )
    }
}