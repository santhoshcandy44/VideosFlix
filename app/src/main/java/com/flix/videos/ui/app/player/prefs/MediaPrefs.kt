package com.flix.videos.ui.app.player.prefs

import android.content.Context
import android.content.SharedPreferences
import org.koin.core.annotation.Factory
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Factory
class MediaPrefs(applicationContext: Context) {

    private val prefs =
        applicationContext.getSharedPreferences("media_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SEEN_VIDEO_IDS = "seen_video_ids"
    }

    private val _changes = MutableStateFlow(emptySet<Long>())
    val changes = _changes.asStateFlow()

    private val sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SEEN_VIDEO_IDS) {
                _changes.value = getSeenVideoIds().toSet()
            }
        }

    fun hasSnapshot(): Boolean {
        return prefs.contains(KEY_SEEN_VIDEO_IDS)
    }

    fun getSeenVideoIds(): MutableSet<Long> {
        return prefs.getStringSet(KEY_SEEN_VIDEO_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toMutableSet()
            ?:  mutableSetOf()
    }

    fun saveSeenVideoIds(ids: Set<Long>) {
        prefs.edit {
            putStringSet(
                KEY_SEEN_VIDEO_IDS,
                ids.map { it.toString() }.toSet()
            )
        }
    }

    fun markVideoAsSeen(id: Long) {
        val seen = getSeenVideoIds()
        if (seen.add(id)) {
            prefs.edit {
                putStringSet(
                    KEY_SEEN_VIDEO_IDS,
                    seen.map { it.toString() }.toSet()
                )
            }
        }
    }

    fun clear() {
        prefs.edit {clear()}
    }

    fun registerOnSharedPreferenceChangeListener(){
        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun unregisterOnSharedPreferenceChangeListener(){
        prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }
}
