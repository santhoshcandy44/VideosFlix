package com.flix.videos.ui.app.player.observables

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun observeVolumeChanges(
    onVolumeChanged: (Boolean, Int, Int) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType =
                        intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)

                    if (streamType == AudioManager.STREAM_MUSIC) {
                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        onVolumeChanged(false, max, current)
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        )

        val maxVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val initial = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        onVolumeChanged(true, maxVolume, initial)

        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }
}
