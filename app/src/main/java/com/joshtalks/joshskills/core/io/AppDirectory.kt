package com.joshtalks.joshskills.core.io

import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.text.format.DateUtils
import com.joshtalks.joshskills.core.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object AppDirectory {
    const val APP_DIRECTORY = "JoshSkill"
    const val MEDIA_DIRECTORY = "Media"
    private val FORMATTER = SimpleDateFormat("yyyyMMdd", Locale.US)

    const val TODAY_IMAGE_COUNT = "today_image_count"
    const val TODAY_VIDEO_COUNT = "today_video_count"
    const val TODAY_RECORDING_COUNT = "today_recording_count"
    const val TODAY_DOCS_COUNT = "today_docs_count"

    const val TODAY_DATE = "today_date"
    const val APP_SHORT_NAME = "JS"
    const val AUDIO_EXTENSION = ".aac"


    enum class FileType {
        IMAGE_SENT, IMAGE_RECEIVED, RECORDING_SENT, RECORDING_RECEIVED, VIDEO_SENT, VIDEO_RECEIVED, DOCS_RECEIVED
    }


    fun getRootDirectoryPath(): File {

        val f = File(
            Environment.getStorageDirectory().path.toString() + File.separator + APP_DIRECTORY
        )
        if (f.exists().not()) {
            f.mkdirs()
        }
        return f
    }

    /**
     * this code is using for received audio file
     **/

    private val AUDIO_RECEIVED_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppAudio/"

    fun getAudioReceivedFile(path: String): File {
        val rootPath = AUDIO_RECEIVED_MIGRATED_PATH
        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + Utils.getFileNameFromURL(path))
        if (file.exists()) {
            return file
        }
        file.createNewFile()
        return file
    }


    /**
     * this code is using for sent audio file
     *
     * */
    private val AUDIO_SENT_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppAudio/Sent"

    fun getAudioSentFile(path: String?, audioExtension: String = AUDIO_EXTENSION): File {

        val rootPath = AUDIO_SENT_MIGRATED_PATH
        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        var fileName = EMPTY

        if (path.isNullOrEmpty().not()) {
            fileName = File(path!!).name
        }

        if (fileName.isEmpty()) {
            fileName = "Record_" + System.currentTimeMillis().toString() + audioExtension
        }
        val file = File(rootPath + File.separator + fileName)
        file.createNewFile()
        return file
    }


    /**
     * this code is using for received audio file
     **/
    private val IMAGE_RECEIVED_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppImages/"

    fun getImageReceivedFile(path: String?): File {
        val rootPath = IMAGE_RECEIVED_MIGRATED_PATH

        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + Utils.getFileNameFromURL(path))
        if (file.exists()) {
            return file
        }
        file.createNewFile()
        return file
    }


    /**
     * this code is using for sent file
     **/
    private val FILE_SENT_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppFiles/Sent"

    fun getSentFile(fileName: String): File {
        val rootPath = FILE_SENT_MIGRATED_PATH

        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + fileName)
        if (file.exists()) {
            return file
        }
        file.createNewFile()
        return file
    }


    /**
     * this code is using for sent audio file
     *
     * */
    private val IMAGE_SENT_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppImages/Sent"

    fun getImageSentFile(path: String?): File {
        val rootPath = IMAGE_SENT_MIGRATED_PATH
        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }

        var fileName = EMPTY
        if (path.isNullOrEmpty().not()) {
            fileName = File(path!!).name
        }

        if (fileName.isEmpty()) {
            fileName = "IMG" + System.currentTimeMillis().toString() + ".jpeg"
        }
        val file = File(rootPath + File.separator + fileName)
        file.createNewFile()
        return file
    }

    private val DOCS_RECEIVED_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppDocuments/"


    fun docsReceivedFile(url: String): File {
        val rootPath = DOCS_RECEIVED_MIGRATED_PATH

        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + Utils.getFileNameFromURL(url))
        file.createNewFile()
        return file
    }

    private val VIDEO_RECEIVED_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppVideos/"


    @JvmStatic
    fun getVideoDownloadDirectory(): File {
        val rootPath = VIDEO_RECEIVED_MIGRATED_PATH

        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        return f
    }


    val VIDEO_SENT_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppVideos/Sent"


    val VIDEO_CACHED_RECEIVED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshApp/cached"

    val TEMP_MIGRATED_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppTemp"

    val KEY_PATH =
        AppObjectController.joshApplication.getExternalFilesDir(null)
            .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/Keys"


    private fun getImageFileName(): String {
        return "IMG".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            FileType.IMAGE_SENT
        ) + ".jpg"
    }

    private fun getVideoFileName(): String {
        return "VID".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            FileType.IMAGE_SENT
        ) + ".mp4"
    }

    private fun getDocsFileName(): String {
        return "DOCS".plus("-").plus(getDate()).plus("-".plus(APP_SHORT_NAME)) + getFileEndName(
            FileType.DOCS_RECEIVED
        ) + ".pdf"
    }

    private fun getDate() = FORMATTER.format(Date())


    private fun getFileEndName(fileType: FileType): String {
        return when (fileType) {
            FileType.IMAGE_SENT -> getFileTodayCount(TODAY_IMAGE_COUNT)
            FileType.IMAGE_RECEIVED -> getFileTodayCount(TODAY_IMAGE_COUNT)
            FileType.RECORDING_SENT -> getFileTodayCount(TODAY_RECORDING_COUNT)
            FileType.RECORDING_RECEIVED -> getFileTodayCount(TODAY_RECORDING_COUNT)
            FileType.VIDEO_SENT -> getFileTodayCount(TODAY_VIDEO_COUNT)
            FileType.VIDEO_RECEIVED -> getFileTodayCount(TODAY_VIDEO_COUNT)
            FileType.DOCS_RECEIVED -> getFileTodayCount(TODAY_DOCS_COUNT)

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
        val rootPath = IMAGE_SENT_MIGRATED_PATH
        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + getImageFileName())
        file.createNewFile()
        return file
    }


    fun getImageSentFilePath(): String {
        return imageSentFile().absolutePath
    }

    fun imageReceivedFile(): File {
        val rootPath = IMAGE_RECEIVED_MIGRATED_PATH
        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + getImageFileName())
        file.createNewFile()
        return file
    }


    fun videoReceivedFile(): File {
        val rootPath = VIDEO_RECEIVED_MIGRATED_PATH

        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + getVideoFileName())
        file.createNewFile()
        return file
    }

    fun videoSentFile(): File {
        val rootPath = VIDEO_SENT_MIGRATED_PATH

        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        val file = File(rootPath + File.separator + getVideoFileName())
        file.createNewFile()
        return file
    }

    fun tempRecordingFile(): File {
        val outputDir =
            AppObjectController.joshApplication.cacheDir // context being the Activity pointer
        return File.createTempFile("record", AUDIO_EXTENSION, outputDir)
    }

    fun tempRecordingFileM4A(): File {
        val outputDir =
            AppObjectController.joshApplication.cacheDir // context being the Activity pointer
        return File.createTempFile("record", ".m4a", outputDir)
    }

    fun deleteRecordingFile() {
        try {
            File.createTempFile(
                "record",
                AUDIO_EXTENSION,
                AppObjectController.joshApplication.cacheDir
            )
                .delete()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getCacheFile(fileName: String, fileExtension: String): File {
        return File.createTempFile(
            fileName,
            fileExtension,
            AppObjectController.joshApplication.externalCacheDir
        )
    }


    fun deleteFile(filePath: String?): Boolean {
        JoshSkillExecutors.BOUNDED.execute {
            if (filePath.isNullOrEmpty()) {
                return@execute
            }
            File(filePath).deleteRecursively()
        }
        return true
    }

    fun deleteFileFile(file: File): Boolean {
        return file.delete()
    }

    @JvmStatic
    fun isFileExist(path: String?): Boolean {
        if (path.isNullOrEmpty()) {
            return false
        }
        return try {
            File(path).exists()
        } catch (e: java.lang.Exception) {
            false
        }

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
            e.printStackTrace()
            return false
        } finally {
            closeSilently(inStream)
            closeSilently(outStream)
        }
        return true
    }


    private fun closeSilently(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: IOException) {
        }
    }

    fun getTempPath(): String {
        val rootPath = TEMP_MIGRATED_PATH

        val f = File(rootPath)
        if (f.exists().not()) {
            f.mkdirs()
        }
        return rootPath

    }


    fun copy2(sourceUri: Uri, file: File): File? {

        val inputPFD: ParcelFileDescriptor? =
            AppObjectController.joshApplication.contentResolver.openFileDescriptor(sourceUri, "r")
        val inStream: FileInputStream? = FileInputStream(inputPFD!!.fileDescriptor)
        var outStream: FileOutputStream? = null
        try {
            outStream = FileOutputStream(file)
            val inChannel = inStream!!.channel
            val outChannel = outStream.channel
            inChannel.transferTo(0, inChannel.size(), outChannel)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            closeSilently(inStream)
            closeSilently(outStream)
        }
        return file
    }

    fun getDirSize(dir: File?): Long {
        var size: Long = 0
        dir?.listFiles()?.forEach { file ->
            if (file != null && file.isDirectory) {
                size += getDirSize(file)
            } else if (file != null && file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    fun getFileSize(file: File?): Long {
        return file?.length() ?: 0
    }
}