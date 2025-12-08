package com.flix.videos.ui.app.player.observables

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun observeSystemVolume(
    onVolumeChanged: (Int, Int) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                onVolumeChanged(maxVolume, currentVolume)
            }
        }

        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            observer
        )

        val maxVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val initial = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        onVolumeChanged(maxVolume, initial)

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
}
