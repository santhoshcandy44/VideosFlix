package com.flix.videos.viewmodel

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
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

@KoinViewModel
class ReadMediaVideosViewModel(val applicationContext: Context) : ViewModel() {
    val videosBackstack = NavBackStack<NavKey>(NavigationBarRoutes.Videos)
    val albumsBackStack = NavBackStack<NavKey>(NavigationBarRoutes.Albums)

    private val _videoInfos = MutableStateFlow<List<VideoInfo>>(emptyList())
    val videoInfos = _videoInfos.asStateFlow()

    val groupedVideos =
        _videoInfos
            .map { items ->
                items.groupBy { File(it.path).parent ?: "Unknown" }
                    .mapValues { (parentPath, list) ->
                        list.map { video ->
                            video.copy(
                                displayGroupName = File(parentPath).name
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

    init {
        applicationContext.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaVideoObserver
        )
        fetchVideoInfos()
    }

    fun fetchVideoInfos() {
        viewModelScope.launch {
            _videoInfos.value = getAllVideos(applicationContext)
        }
    }

    fun getAllVideos(context: Context): List<VideoInfo> {
        val videos = mutableListOf<VideoInfo>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->

            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val mimetypeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(displayNameCol)
                val duration = cursor.getLong(durCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val size = cursor.getLong(sizeCol)
                val data = cursor.getString(dataCol)
                val mimetype = cursor.getString(mimetypeCol)

                val dateAdded = cursor.getLong(dateAddedCol)

                val uri = ContentUris.withAppendedId(collection, id)

                // FAST thumbnail
                val thumbnail = try {
                    context.contentResolver.loadThumbnail(uri, Size(300, 300), null)
                } catch (_: Exception) {
                    null
                }

                videos.add(
                    VideoInfo(
                        uri,
                        data,
                        displayName,
                        displayName.substringBeforeLast(".", ""),
                        width,
                        height,
                        duration,
                        size,
                        mimetype,
                        dateAdded,
                        thumbnail
                    )
                )
            }
        }
        return videos
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
    }
}