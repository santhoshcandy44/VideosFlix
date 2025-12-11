package com.flix.videos.ui.app.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.flix.videos.models.VideoInfo
import com.flix.videos.ui.app.bottombar.NavigationBarRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.io.File

enum class ViewMode {
    LIST,
    GRID
}

@KoinViewModel
class ReadMediaVideosViewModel(val applicationContext: Context, val mediaSourceRepository: MediaSourceRepository) : ViewModel() {
    val videosBackstack = NavBackStack<NavKey>(NavigationBarRoutes.Videos)
    val albumsBackStack = NavBackStack<NavKey>(NavigationBarRoutes.Albums)

    private val _videoInfos = MutableStateFlow<List<VideoInfo>>(emptyList())
    val videoInfos = _videoInfos.asStateFlow()

    private val sharedPrefs =
        applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VIEW_MODE = "videos_view_mode"
    }

    private val _videosViewMode = MutableStateFlow(getViewMode(sharedPrefs))
    val videosViewMode = _videosViewMode.asStateFlow()

    val groupedVideos = _videoInfos
        .map { items ->
            items.groupBy { File(it.path).parent ?: "Unknown"}
                .mapValues { (groupParent, list) ->
                    list.map { video ->
                        val groupParentFile = File(groupParent)
                        val absPath = groupParentFile.absolutePath
                        val root = Environment.getExternalStorageDirectory().absolutePath
                        video.copy(
                            displayGroupName = if (absPath == root) "Root" else groupParentFile.name
                        )
                    }
                }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyMap()
        )

    val mediaVideoObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            fetchVideoInfos()
        }
    }
    private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_VIEW_MODE) {
                _videosViewMode.value = getViewMode(prefs)
            }
        }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        applicationContext.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaVideoObserver
        )
        fetchVideoInfos()
    }

    private fun getViewMode(prefs: SharedPreferences): ViewMode {
        val name = prefs.getString(KEY_VIEW_MODE, ViewMode.LIST.name)!!
        return ViewMode.valueOf(name)
    }

    fun saveVideoViewMode(mode: ViewMode) {
        sharedPrefs.edit { putString(KEY_VIEW_MODE, mode.name) }
        _videosViewMode.value = mode
    }

    fun fetchVideoInfos() {
        viewModelScope.launch {
            _videoInfos.value = mediaSourceRepository.getAllVideos()
        }
    }

    fun renameVideo(
        videoUri: Uri,
        newNameWithoutExtension: String
    ): Boolean {
        val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME)
        val cursor =
            applicationContext.contentResolver.query(videoUri, projection, null, null, null)

        val currentName = cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: return false

        val extension = currentName.substringAfterLast('.', missingDelimiterValue = "")
        val newFileName = if (extension.isNotEmpty()) {
            "$newNameWithoutExtension.$extension"
        } else {
            newNameWithoutExtension
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, newFileName)
        }

        val rows = applicationContext.contentResolver.update(videoUri, values, null, null)
        applicationContext.contentResolver.notifyChange(videoUri, null)
        return rows > 0
    }

    override fun onCleared() {
        super.onCleared()
        applicationContext.contentResolver.unregisterContentObserver(mediaVideoObserver)
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }
}