package com.joshtalks.joshskills.core.io

import android.os.Environment
import android.text.format.DateUtils
import com.joshtalks.joshskills.core.PrefManager
import java.text.SimpleDateFormat
import java.util.*
import java.io.*


object AppDirectory {
    const val APP_DIRECTORY = "JoshSkill"
    const val MEDIA_DIRECTORY = "Media"
    private val FORMATTER = SimpleDateFormat("yyyyMMdd")

    const val TODAY_IMAGE_COUNT = "today_image_count"
    const val TODAY_VIDEO_COUNT = "today_video_count"
    const val TODAY_RECORDING_COUNT = "today_recording_count"
    const val TODAY_DATE = "today_date"
    const val APP_SHORT_NAME = "JS"


    enum class FileType {
        IMAGE_SENT, IMAGE_RECEIVED, RECORDING_SENT, RECORDING_RECEIVED, VIDEO_SENT, VIDEO_RECEIVED
    }


    val IMAGE_SENT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Images/Sent"

    val IMAGE_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Images/"


    val RECORDING_SENT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Recordings/Sent"

    val RECORDING_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Recordings/"


    val VIDEO_SENT_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Videos/Sent"

    val VIDEO_RECEIVED_PATH =
        Environment.getExternalStorageDirectory().toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp Videos/"


    private fun getImageFileName(): String {
        return "IMG".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            AppDirectory.FileType.IMAGE_SENT
        ) + ".jpg"
    }

    private fun getVideoFileName(): String {
        return "VID".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            AppDirectory.FileType.IMAGE_SENT
        ) + ".mp4"
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

    fun getTempImageFile() {

    }

    fun deleteFile(file: File): Boolean {
        return file.delete()

    }

    fun deleteFile(filePath: String): Boolean {
        return File(filePath).delete()

    }

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
}