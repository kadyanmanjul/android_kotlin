package com.joshtalks.joshskills.ui.special_practice.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import androidx.camera.video.VideoRecordEvent
import com.joshtalks.joshskills.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

fun doesVideoHaveAudio(path: String): Boolean {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
}

fun getVideoDuration(context: Context, file: File): Long {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, Uri.fromFile(file))
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val timeInMillis = time?.toLong() ?: 0
    retriever.release()
    return timeInMillis
}

fun VideoRecordEvent.getNameString(): String {
    return when (this) {
        is VideoRecordEvent.Status -> "Status"
        is VideoRecordEvent.Start -> "Started"
        is VideoRecordEvent.Finalize -> "Finalized"
        is VideoRecordEvent.Pause -> "Paused"
        is VideoRecordEvent.Resume -> "Resumed"
        else -> throw IllegalArgumentException("Unknown VideoRecordEvent: $this")
    }
}

fun getWindowWidth(context: Context): Int {
    val display =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val size = Point()
    display.getSize(size)
    return size.x
}

fun getWindowHeight(context: Context): Int {
    val display =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val size = Point()
    display.getSize(size)
    return size.y
}

fun getVideoFilePath(): String {
    return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + SimpleDateFormat(
        "ddMMyyyy",
        Locale.US
    ).format(System.currentTimeMillis()) + ".mp4"
}

fun getAndroidDownloadFolder(): File? {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
}

fun getAppShareUrl(userReferralCode: String): String {
    return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
}

fun exportMp4ToGallery(context: Context, filePath: String) {
    try {
        val values = ContentValues(2)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, filePath)
        context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        )
        context.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://$filePath")
            )
        )
    } catch (ex: Exception) {
    }
}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).toInt()
}

fun deleteFile(videoName: String) {
    val imageFile =
        File(Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_MOVIES + File.separator + videoName)
    if (imageFile.exists()) {
        imageFile.delete()
    }
}

fun convertImageFilePathIntoBitmap(imageFile: String): Bitmap {
    return BitmapFactory.decodeFile(imageFile)
}

fun getHeightByPixel(context: Context): Int {
    var height = 0
    height = if ((pxToDp(getWindowHeight(context)) % 2) == 0) {
        ((pxToDp(getWindowHeight(context))) * 2).minus(130)
    } else {
        ((pxToDp(getWindowHeight(context)) + 1) * 2).minus(130)
    }
    return height
}

fun getRecordingFileName(): String {
    return "JoshSkill-Recording-" +
            SimpleDateFormat("ddMMyyyy", Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"
}
