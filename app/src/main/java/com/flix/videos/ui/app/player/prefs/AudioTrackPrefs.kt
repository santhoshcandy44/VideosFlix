package com.flix.videos.ui.app.player.prefs

import android.content.Context
import android.net.Uri
import org.koin.core.annotation.Factory

@Factory
class AudioTrackPrefs(applicationContext: Context) {

    private val prefs = applicationContext.getSharedPreferences("audio_track_prefs", Context.MODE_PRIVATE)

    fun saveAudioTrack(videoUri: Uri, groupIndex: Int, trackIndex: Int) {
        val key = "audio_track_$videoUri"
        val value = "$groupIndex:$trackIndex"

        prefs.edit()
            .putString(key, value)
            .apply()
    }

    fun getSavedAudioTrack(videoUri: Uri): Pair<Int, Int>? {
        val key = "audio_track_$videoUri"
        val value = prefs.getString(key, null) ?: return null

        val parts = value.split(":")
        if (parts.size != 2) return null

        val g = parts[0].toIntOrNull() ?: return null
        val t = parts[1].toIntOrNull() ?: return null

        return g to t
    }

    fun clearAudioTrack(videoUri: Uri) {
        prefs.edit().remove(videoUri.toString()).apply()
    }
}
