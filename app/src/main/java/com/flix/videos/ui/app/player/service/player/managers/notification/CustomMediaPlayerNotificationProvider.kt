package com.flix.videos.ui.app.player.service.player.managers.notification

import android.R
import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.collect.ImmutableList

class CustomMediaPlayerNotificationProvider(context: Context) :
    MediaNotification.Provider {

    private val context = context.applicationContext

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {

        val builder = NotificationCompat.Builder(context,
            MediaPlayerNotificationManager.Companion.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_media_play)
            .setContentTitle(mediaSession.player.mediaMetadata.title)
            .setContentText(mediaSession.player.mediaMetadata.artist)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val previousAction = actionFactory.createMediaAction(
            mediaSession,
            IconCompat.createWithResource(context, com.flix.videos.R.drawable.ic_video_backward),
            "Previous",
            Player.COMMAND_SEEK_TO_PREVIOUS
        )

        val playPause = if (mediaSession.player.isPlaying) {
            actionFactory.createMediaAction(
                mediaSession,
                IconCompat.createWithResource(
                    context,
                    com.flix.videos.R.drawable.ic_video_pause
                ),
                "Play",
                Player.COMMAND_PLAY_PAUSE,
            )
        } else {
            actionFactory.createMediaAction(
                mediaSession,
                IconCompat.createWithResource(
                    context,
                    com.flix.videos.R.drawable.ic_video_play
                ),
                "Pause",
                Player.COMMAND_PLAY_PAUSE
            )
        }

        val nextAction = actionFactory.createMediaAction(
            mediaSession,
            IconCompat.createWithResource(context, com.flix.videos.R.drawable.ic_video_forward),
            "Previous",
            Player.COMMAND_SEEK_TO_PREVIOUS
        )

        builder.addAction(previousAction)
        builder.addAction(playPause)
        builder.addAction(nextAction)

        builder.setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession).setShowActionsInCompactView(0, 1, 2))

        return MediaNotification(
            MediaPlayerNotificationManager.Companion.NOTIFICATION_ID,
            builder.build()
        )
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = true
}