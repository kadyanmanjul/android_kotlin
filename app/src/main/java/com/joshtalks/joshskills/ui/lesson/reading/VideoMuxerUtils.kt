package com.joshtalks.joshskills.ui.lesson.reading

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.greentoad.turtlebody.mediapicker.util.UtilsFile
import com.joshtalks.joshskills.core.showToast
import java.io.File
import java.io.FileOutputStream

@RequiresApi(api = Build.VERSION_CODES.Q)
fun saveVideoQ(ctx: Context, videoPath: String): String? {
    try {
        val valuesVideos = ContentValues()
        val videoFileName = "MergedVideo_" + System.currentTimeMillis() + ".mp4"
        valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/")
        valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName)
        valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
        valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        val uriSavedVideo = ctx.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            valuesVideos
        )
        val pfd: ParcelFileDescriptor?
        try {
            pfd = uriSavedVideo?.let { ctx.contentResolver.openFileDescriptor(it, "w") }
            assert(pfd != null)
            val out = FileOutputStream(pfd!!.fileDescriptor)

            val inputStream = ctx.contentResolver.openInputStream(Uri.parse(videoPath))
            val buf = ByteArray(8192)
            var len: Int
            var progress = 0
            while (inputStream!!.read(buf).also { len = it } > 0) {
                progress += len
                out.write(buf, 0, len)
            }
            out.close()
            inputStream.close()
            pfd.close()
            valuesVideos.clear()
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 0)
            valuesVideos.put(
                MediaStore.Video.Media.IS_PENDING,
                0
            )
            uriSavedVideo?.let { ctx.contentResolver.update(it, valuesVideos, null, null) }
            return uriSavedVideo?.let { UtilsFile.getFilePath(ctx, it) }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }catch (ex:Exception){
        showToast(ex.message.toString())
    }
    return getVideoFilePath()
}

fun getVideoFilePath(): String {
    return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp4"
}

fun getAndroidDownloadFolder(): File? {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
}
