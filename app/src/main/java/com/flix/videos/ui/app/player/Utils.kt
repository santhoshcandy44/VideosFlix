package com.flix.videos.ui.app.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.flix.videos.ui.utils.findActivity

fun enterFullScreenMode(activity: Activity, enableSwipeUp: Boolean = true) {
    WindowInsetsControllerCompat(
        activity.window,
        activity.window.decorView
    ).apply {
        hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        if (enableSwipeUp)
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun exitFullScreenMode(activity: Activity) {
    WindowInsetsControllerCompat(
        activity.window,
        activity.window.decorView
    ).apply {
        show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun getCurrentVolume(context: Context): Float {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    return current.toFloat() / max.toFloat()   // 0f to 1f
}

fun setSystemVolume(context: Context, progress: Float): Float {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val newVolume = (progress * max).toInt().coerceIn(0, max)

    audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        newVolume,
        0
    )

    return newVolume.toFloat() / max
}

fun getCurrentWindowBrightness(context: Context): Float {
    val activity = context.findActivity()
    val brightness = activity.window.attributes.screenBrightness

    return if (brightness < 0f) {
        // App not overriding brightness → return system brightness as fallback
        getSystemBrightness(context)
    } else {
        brightness
    }
}

private fun getSystemBrightness(context: Context): Float {
    return Settings.System.getInt(
        context.contentResolver,
        Settings.System.SCREEN_BRIGHTNESS
    ) / 255f
}

fun updateBrightness(context: Context, progress: Float) {
    val activity = context.findActivity()
    val params = activity.window.attributes

    params.screenBrightness = progress.coerceIn(0f, 1f)
    activity.window.attributes = params
}

