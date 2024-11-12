package com.and04.naturealbum.ui.album

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.and04.naturealbum.data.room.PhotoDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun saveImageToGallery(context: Context, photoDetails: List<PhotoDetail>) {
    val resolver = context.contentResolver
    CoroutineScope(Dispatchers.IO).launch {
        photoDetails.forEach { photoDetail ->
            val contentValues = getContentValues(photoDetail)
            val externalUri = getExternalUri(contentValues, resolver, photoDetail)
            if (externalUri != null) {
                try {
                    resolver.openInputStream(photoDetail.photoUri.toUri()).use { inputStream ->
                        resolver.openOutputStream(externalUri).use { outputStream ->
                            if (inputStream != null && outputStream != null) {
                                copyStream(inputStream, outputStream)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(externalUri, null, null) // 오류 발생 시 생성된 항목 삭제
                }
            }
        }
    }
}

private fun getContentValues(photoDetail: PhotoDetail): ContentValues {
    return ContentValues().apply {
        put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            "NatureAlbum_${photoDetail.description.replace("\n", "")}"
        )
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DCIM}/NatureAlbum"
            )
        } else {
            val directory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + "/NatureAlbum"
            val file = File(directory)
            if (!file.exists()) file.mkdirs()
        }
    }
}

private fun getExternalUri(
    contentValues: ContentValues,
    resolver: ContentResolver,
    photoDetail: PhotoDetail,
): Uri? {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        val file = File(
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/NatureAlbum",
            "NatureAlbum_${photoDetail.description.replace("\n", "")}.jpg"
        )
        file.toUri()
    }
}

private fun copyStream(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(1024)
    var bytesRead: Int
    while (input.read(buffer).also { bytesRead = it } != -1) {
        output.write(buffer, 0, bytesRead)
    }
}
