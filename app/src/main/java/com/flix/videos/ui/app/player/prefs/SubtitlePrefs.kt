package com.flix.videos.ui.app.player.prefs

import android.content.Context
import android.net.Uri
import org.koin.core.annotation.Factory

@Factory
class SubtitlePrefs(applicationContext: Context) {
    private val prefs = applicationContext.getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)

    fun saveSubtitleUri(videoUri: Uri, subtitleUri: Uri?) {
        prefs.edit()
            .putString("subtitle_uri_$videoUri", subtitleUri.toString())
            .apply()
    }

    fun getSavedSubtitleUri(videoUri: Uri): Uri? {
        val s = prefs.getString("subtitle_uri_$videoUri", null) ?: return null
        return Uri.parse(s)
    }

    fun clearSubtitle(videoUri: Uri) {
        prefs.edit().remove("subtitle_uri_$videoUri").apply()
    }

    fun clearAll() {
        prefs.edit()
            .clear()
            .apply()
    }
}
