package com.flix.videos.ui.app.player.prefs

import android.content.Context
import android.net.Uri
import com.flix.videos.ui.app.player.ExoPlayerRepeatMode
import org.koin.core.annotation.Factory

@Factory
class PlaybackSettingsPrefs(applicationContext: Context) {

    private val prefs = applicationContext.getSharedPreferences(
        "playback_settings_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val AUDIO_ONLY = "is_audio_only"
        private const val SUBTITLES_ENABLED = "subtitles_enabled"
        private const val PLAYBACK_MODE = "playback_mode"

        private const val PLAYBACK_SPEED = "playback_speed"
    }

    /** Save audio-only mode */
    fun setAudioOnly(enabled: Boolean) {
        prefs.edit()
            .putBoolean(AUDIO_ONLY, enabled)
            .apply()
    }

    /** Load audio-only mode */
    fun isAudioOnly(): Boolean {
        return prefs.getBoolean(AUDIO_ONLY, false)
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(SUBTITLES_ENABLED, enabled)
            .apply()
    }

    fun isSubtitlesEnabled(): Boolean {
        return prefs.getBoolean(SUBTITLES_ENABLED, true) // default = enabled
    }

    fun setPlaybackMode(mode: ExoPlayerRepeatMode) {
        prefs.edit()
            .putString(PLAYBACK_MODE, mode.name)
            .apply()
    }

    fun getPlaybackMode(): ExoPlayerRepeatMode {
        val saved = prefs.getString(PLAYBACK_MODE, ExoPlayerRepeatMode.REPEAT_MODE_OFF.name)
        return ExoPlayerRepeatMode.valueOf(saved ?: ExoPlayerRepeatMode.REPEAT_MODE_OFF.name)
    }

    fun setPlaybackSpeed(speed: Float) {
        prefs.edit()
            .putFloat(PLAYBACK_SPEED, speed)
            .apply()
    }

    fun getPlaybackSpeed(): Float {
        return prefs.getFloat(PLAYBACK_SPEED, 1.0f) // default = normal speed
    }
}
