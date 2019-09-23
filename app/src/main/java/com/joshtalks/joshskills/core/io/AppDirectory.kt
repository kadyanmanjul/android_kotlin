package com.joshtalks.joshskills.core.io

import android.os.Environment
import android.text.format.DateUtils
import com.joshtalks.joshskills.core.PrefManager
import java.text.SimpleDateFormat
import java.util.*
import java.io.*
import com.facebook.FacebookSdk.getCacheDir
import com.joshtalks.joshskills.core.AppObjectController
import com.facebook.FacebookSdk.getCacheDir


object AppDirectory {
    const val APP_DIRECTORY = "JoshSkill"
    const val MEDIA_DIRECTORY = "Media"
    private val FORMATTER = SimpleDateFormat("yyyyMMdd")

    const val TODAY_IMAGE_COUNT = "today_image_count"
    const val TODAY_VIDEO_COUNT = "today_video_count"
    const val TODAY_RECORDING_COUNT = "today_recording_count"
    const val TODAY_DOCS_COUNT = "today_docs_count"

    const val TODAY_DATE = "today_date"
    const val APP_SHORT_NAME = "JS"


    enum class FileType {
        IMAGE_SENT, IMAGE_RECEIVED, RECORDING_SENT, RECORDING_RECEIVED, VIDEO_SENT, VIDEO_RECEIVED, DOCS_RECEIVED
    }


    val IMAGE_SENT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Images/Sent"

    val IMAGE_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Images/"


    val DOCS_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Documents/"


    val RECORDING_SENT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Recordings/Sent"

    val RECORDING_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Recordings/"


    val VIDEO_SENT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Videos/Sent"

    val VIDEO_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Videos/"
    val VIDEO_CACHED_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp/cached"


    val TEMP_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Temp"


    private fun getImageFileName(): String {
        return "IMG".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            AppDirectory.FileType.IMAGE_SENT
        ) + ".jpg"
    }

    private fun getAudioFileName(): String {
        return "RECORD".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            AppDirectory.FileType.RECORDING_SENT
        ) + ".amr"
    }

    private fun getAudioFileName(extension: String): String {
        return "RECORD".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            AppDirectory.FileType.RECORDING_SENT
        ) + extension
    }

    private fun getVideoFileName(): String {
        return "VID".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            AppDirectory.FileType.IMAGE_SENT
        ) + ".mp4"
    }

    private fun getDocsFileName(): String {
        return "DOCS".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            AppDirectory.FileType.DOCS_RECEIVED
        ) + ".pdf"
    }

    private fun getDate() = FORMATTER.format(Date())


    private fun getFileEndName(fileType: FileType): String {
        return when (fileType) {
            AppDirectory.FileType.IMAGE_SENT -> getFileTodayCount(TODAY_IMAGE_COUNT)
            AppDirectory.FileType.IMAGE_RECEIVED -> getFileTodayCount(TODAY_IMAGE_COUNT)
            AppDirectory.FileType.RECORDING_SENT -> getFileTodayCount(TODAY_RECORDING_COUNT)
            AppDirectory.FileType.RECORDING_RECEIVED -> getFileTodayCount(TODAY_RECORDING_COUNT)
            AppDirectory.FileType.VIDEO_SENT -> getFileTodayCount(TODAY_VIDEO_COUNT)
            AppDirectory.FileType.VIDEO_RECEIVED -> getFileTodayCount(TODAY_VIDEO_COUNT)
            AppDirectory.FileType.DOCS_RECEIVED -> getFileTodayCount(TODAY_DOCS_COUNT)

        }


    }


    private fun getFileTodayCount(prefName: String): String {
        val lDate = Date(PrefManager.getLongValue(TODAY_DATE))

        return run {
            if (DateUtils.isToday(lDate.time).not()) {
                PrefManager.put(TODAY_DATE, Date().time)
            }
            val c = 1 + PrefManager.getIntValue(prefName)
            PrefManager.put(prefName, c)
            String.format("%04d", c)

        }
    }


    fun imageSentFile(): File {
        val f = File(IMAGE_SENT_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(IMAGE_SENT_PATH + File.separator + getImageFileName())
        file.createNewFile();
        return file
    }


    fun getImageSentFilePath(): String {
        return imageSentFile().absolutePath
    }

    fun imageReceivedFile(): File {
        val f = File(IMAGE_SENT_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(IMAGE_RECEIVED_PATH + File.separator + getImageFileName())
        file.createNewFile();
        return file
    }


    fun docsReceivedFile(): File {
        val f = File(DOCS_RECEIVED_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(DOCS_RECEIVED_PATH + File.separator + getDocsFileName())
        file.createNewFile()
        return file
    }


    fun videoReceivedFile(): File {
        val f = File(VIDEO_RECEIVED_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(VIDEO_RECEIVED_PATH + File.separator + getVideoFileName())
        file.createNewFile()
        return file
    }

    fun videoSentFile(): File {
        val f = File(VIDEO_SENT_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(VIDEO_SENT_PATH + File.separator + getVideoFileName())
        file.createNewFile()
        return file
    }


    fun recordingSentFile(): File {
        val f = File(RECORDING_SENT_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(RECORDING_SENT_PATH + File.separator + getAudioFileName())
        file.createNewFile();
        return file
    }


    fun getRecordingReceivedFilePath(): String {
        return recordingSentFile().absolutePath
    }


    fun recordingReceivedFile(fileName: String): File {
        val extension = fileName.substring(fileName.lastIndexOf("."))
        val f = File(RECORDING_RECEIVED_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(RECORDING_RECEIVED_PATH + File.separator + getAudioFileName(extension))
        file.createNewFile();
        return file
    }

    fun tempRecordingFile(): File {
        val outputDir =
            AppObjectController.joshApplication.cacheDir // context being the Activity pointer
        return File.createTempFile("record", ".amr", outputDir)
    }

    fun tempImageFile(): File {
        val outputDir =
            AppObjectController.joshApplication.cacheDir // context being the Activity pointer
        return File.createTempFile("image", ".jpg", outputDir)
    }

    fun tempRecordingWavFile(): File {
        var file: File
        try {
            val f = File(RECORDING_SENT_PATH)
            if (f.exists().not()) {
                f.mkdirs()
            }
            file = File(RECORDING_SENT_PATH + File.separator + "record.amr")
            file.createNewFile();
        } catch (ex: Exception) {
            file = getCacheFile("record", ".amr")
        }
        return file
    }

    fun getCacheFile(fileName: String, fileExtension: String): File {
        return File.createTempFile(
            fileName,
            fileExtension,
            AppObjectController.joshApplication.externalCacheDir
        )
    }

    fun tempRecordingVideoFile(): File {
        val f = File(RECORDING_SENT_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(RECORDING_SENT_PATH + File.separator + "record.mp4")
        file.createNewFile();
        return file
    }


    fun getVideoCacheFolder(): File {
        val f = File(VIDEO_CACHED_RECEIVED_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        return f

    }

    fun getRecordingSentFilePath(): String {
        return recordingSentFile().absolutePath
    }


    fun getTempImageFile() {

    }

    fun deleteFile(file: File): Boolean {
        return file.delete()

    }

    fun deleteFile(filePath: String): Boolean {
        return File(filePath).delete()

    }

    @JvmStatic
    fun isFileExist(path: String): Boolean {
        return File(path).exists()
    }

    fun rename(fromPath: String, toPath: String): Boolean {
        val file = File(fromPath)
        val newFile = File(toPath)
        return file.renameTo(newFile)
    }

    fun copy(fromPath: String, toPath: String): Boolean {
        val file = File(fromPath)
        if (!file.isFile) {
            return false
        }
        var inStream: FileInputStream? = null
        var outStream: FileOutputStream? = null
        try {
            inStream = FileInputStream(file)
            outStream = FileOutputStream(File(toPath))
            val inChannel = inStream.channel
            val outChannel = outStream.channel
            inChannel.transferTo(0, inChannel.size(), outChannel)
        } catch (e: Exception) {
            return false
        } finally {
            closeSilently(inStream)
            closeSilently(outStream)
        }
        return true
    }


    private fun closeSilently(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable!!.close()
            } catch (e: IOException) {
            }

        }
    }

    fun getTempPath(): String {
        val f = File(TEMP_PATH)
        if (f.exists().not()) {
            f.mkdirs()
        }
        return TEMP_PATH

    }

    fun getFilePathForVideoRecordCache(): File {
        var path =
            Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/cached"
        val f = File(path)
        if (f.exists().not()) {
            f.mkdirs()
        }
        return f
    }
}