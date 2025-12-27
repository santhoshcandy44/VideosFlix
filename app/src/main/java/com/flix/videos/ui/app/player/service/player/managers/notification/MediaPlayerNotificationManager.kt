package com.flix.videos.ui.app.player.service.player.managers.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import org.koin.core.annotation.Factory

@Factory
class MediaPlayerNotificationManager(val applicationContext: Context) {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "media_playback"
    }

    fun createMediaPlayBackNotificationChannel() {
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                }
            )
    }
}