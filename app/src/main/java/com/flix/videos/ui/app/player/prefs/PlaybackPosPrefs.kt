package com.flix.videos.ui.app.player.prefs

import android.content.Context
import android.net.Uri
import org.koin.core.annotation.Factory

@Factory
class PlaybackPosPrefs(applicationContext: Context) {

    private val prefs = applicationContext.getSharedPreferences("playback_pos_prefs", Context.MODE_PRIVATE)

    /** Save playback position for a URI */
    fun savePosition(uri: Uri, position: Long) {
        prefs.edit()
            .putLong("pos_${uri}", position)
            .apply()
    }

    /** Get saved playback position for a URI */
    fun getPosition(uri: Uri): Long {
        return prefs.getLong("pos_${uri}", 0L)
    }

    /** Clear saved position for a URI */
    fun clearPosition(uri: Uri) {
        prefs.edit()
            .remove("pos_${uri}")
            .apply()
    }

    /** Clear all saved playback positions */
    fun clearAll() {
        prefs.edit()
            .clear()
            .apply()
    }
}
