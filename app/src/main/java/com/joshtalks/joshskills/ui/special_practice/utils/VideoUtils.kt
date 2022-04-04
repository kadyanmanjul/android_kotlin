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
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.video.VideoRecordEvent
import com.greentoad.turtlebody.mediapicker.util.UtilsFile
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.special_practice.viewmodel.SpecialPracticeViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


fun doesVideoHaveAudio(path: String): Boolean {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == YES
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
        is VideoRecordEvent.Status -> STATUS
        is VideoRecordEvent.Start -> STARTED
        is VideoRecordEvent.Finalize -> FINALIZED
        is VideoRecordEvent.Pause -> PAUSED
        is VideoRecordEvent.Resume -> RESUME
        else -> throw IllegalArgumentException("Unknown VideoRecordEvent: $this")
    }
}

fun getWindowWidth(): Int {
    val display =
        (AppObjectController.joshApplication.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val size = Point()
    display.getSize(size)
    return size.x
}

fun getWindowHeight(): Int {
    val display =
        (AppObjectController.joshApplication.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
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

fun getHeightByPixel(): Int {
    var height = 0
    height = if ((pxToDp(getWindowHeight()) % 2) == 0) {
        ((pxToDp(getWindowHeight())) * 2).minus(130)
    } else {
        ((pxToDp(getWindowHeight()) + 1) * 2).minus(130)
    }
    return height
}

fun getRecordingFileName(): String {
    val fileName: String
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val currentDateTime = sdf.format(Date())
    fileName = "status_" + currentDateTime + Random().nextInt(61) + 20 + ".mp4"
    return  fileName
}

@RequiresApi(api = Build.VERSION_CODES.Q)
fun saveVideoQ(ctx: Context,specialPracticeViewModel: SpecialPracticeViewModel): String? {
    try {
        val valuesVideos = ContentValues()
        val videoFileName = "SpecialVideo_" + System.currentTimeMillis() + ".mp4"
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

            val inputStream = ctx.contentResolver.openInputStream(Uri.parse(specialPracticeViewModel.videoUri.get()))
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