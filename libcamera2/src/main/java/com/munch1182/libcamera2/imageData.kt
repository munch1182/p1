package com.munch1182.libcamera2

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

data class ImageDataModel(
    var uri: Uri,
    val imageId: Long,
    val name: String,
    val title: String,
    val path: String,
    val size: Long,
    val modified: Long,
    val dateAdded: Long,
)

fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
    var res: String? = null
    runCatching {
        val proj = arrayOf(MediaStore.Images.Media.DATA)

        val cursor = context.contentResolver.query(contentUri, proj, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                res = cursor.getString(columnIndex)
            }
        }
        cursor?.close()
    }
    return res
}

@RequiresApi(Build.VERSION_CODES.Q)
fun queryContentResolver(context: Context, result: (List<ImageDataModel>) -> Unit) {
    val listOfImageDataModels = arrayListOf<ImageDataModel>()
    val volume = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    val query = context.contentResolver.query(volume, projection, null, null, null)
    query?.use { cursor ->
        while (cursor.moveToNext()) {
            val imageId: Long = cursor.getColumnLong(MediaStore.Images.Media._ID)
            val name: String = cursor.getColumnString(MediaStore.Images.Media.DISPLAY_NAME)
            val title: String = cursor.getColumnString(MediaStore.Images.Media.TITLE)
            val path: String = cursor.getColumnString(MediaStore.Images.Media.DATA)
            val size: Long = cursor.getColumnLong(MediaStore.Images.Media.SIZE)
            val modified: Long = cursor.getColumnLong(MediaStore.Images.Media.DATE_MODIFIED)
            val dateAdded: Long = cursor.getColumnLong(MediaStore.Images.Media.DATE_ADDED)

            val contentUri: Uri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId)
            listOfImageDataModels.add(
                ImageDataModel(
                    contentUri,
                    imageId,
                    name,
                    title,
                    path,
                    size,
                    modified,
                    dateAdded
                )
            )
        }
    }

    result(listOfImageDataModels)
}

val projection: Array<String> by lazy {
    arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.TITLE,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
    )
}

fun Cursor.getColumnString(mediaColumn: String): String =
    getString(getColumnIndexOrThrow(mediaColumn)) ?: ""

fun Cursor.getColumnLong(mediaColumn: String): Long = getLong(getColumnIndexOrThrow(mediaColumn))