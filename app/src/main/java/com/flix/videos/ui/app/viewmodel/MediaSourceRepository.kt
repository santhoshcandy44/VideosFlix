package com.flix.videos.ui.app.viewmodel

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import androidx.media3.common.MimeTypes
import com.flix.videos.models.VideoInfo
import org.koin.core.annotation.Factory

data class SubtitleFileInfo(
    val id:Long,
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val size: Long
)

@Factory
class MediaSourceRepository(val applicationContext: Context) {
    @Suppress("DEPRECATION")
    fun getAllVideos(): List<VideoInfo> {
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

        applicationContext.contentResolver.query(
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        applicationContext.contentResolver.loadThumbnail(uri, Size(300, 300), null)
                    } else {
                        if (id == -1L)
                            null
                        else
                            MediaStore.Images.Thumbnails.getThumbnail(
                                applicationContext.contentResolver,
                                id,
                                MediaStore.Images.Thumbnails.MINI_KIND,
                                null
                            )
                    }
                } catch (_: Exception) {
                    null
                }

                videos.add(
                    VideoInfo(
                        id,
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

    fun getSubtitleFiles(): List<SubtitleFileInfo> {
        val subtitleExtensions = listOf(
            "srt",      // SubRip
            "vtt",      // WebVTT
            "ssa", "ass", // SubStation Alpha
            "ttml",     // Timed Text Markup (DFXP)
            "xml",      // TTML XML files
            "stl",      // EBU STL
            "dfxp",     // EBU-TT-D (XML based)
            "sbv",      // YouTube captions
            "sub",      // MicroDVD subtitle format
            "lrc"       // Lyrics (supported via TextOutput)
        )

        // Query all files in MediaStore.Downloads + MediaStore.Files
        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE
        )

        val selection = subtitleExtensions.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.${it}'"
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val result = mutableListOf<SubtitleFileInfo>()

        applicationContext.contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mime = cursor.getString(mimeColumn)
                val size = cursor.getLong(sizeColumn)

                val fileUri = ContentUris.withAppendedId(uri, id)

                result.add(
                    SubtitleFileInfo(
                        id = id,
                        uri = fileUri,
                        name = name,
                        mimeType = mime,
                        size = size
                    )
                )
            }
        }

        return result
    }

    fun getSubtitleInfoFromUri(uri: Uri): SubtitleFileInfo? {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE
        )

        applicationContext.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))

                return SubtitleFileInfo(
                    id = id,
                    uri = uri,
                    name = name,
                    mimeType = mime,
                    size = size
                )
            }
        }

        return null
    }

    fun detectSubtitleMimeType(uri: Uri): String {
         val resolverMime = applicationContext.contentResolver.getType(uri)
         if (!resolverMime.isNullOrEmpty()) return resolverMime
        val fileName = getFileName(applicationContext, uri)?.lowercase()
            ?: uri.lastPathSegment?.lowercase() // fallback
            ?: return MimeTypes.TEXT_UNKNOWN
        return when {
            fileName.endsWith(".srt")  -> MimeTypes.APPLICATION_SUBRIP
            fileName.endsWith(".vtt")  -> MimeTypes.TEXT_VTT
            fileName.endsWith(".ssa")  -> MimeTypes.TEXT_SSA   // SSA/ASS both use SSA MIME
            fileName.endsWith(".ass")  -> MimeTypes.TEXT_SSA
            fileName.endsWith(".ttml") -> MimeTypes.APPLICATION_TTML
            fileName.endsWith(".dfxp") -> MimeTypes.APPLICATION_TTML
            fileName.endsWith(".xml")  -> MimeTypes.APPLICATION_TTML  // many TTML files
            fileName.endsWith(".sub")  -> MimeTypes.TEXT_UNKNOWN      // only paired with idx
            fileName.endsWith(".lrc")  -> MimeTypes.TEXT_UNKNOWN      // lyrics not supported
            else -> MimeTypes.TEXT_UNKNOWN
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }

        return null
    }
}