package com.joshtalks.joshskills.util

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import com.joshtalks.joshskills.util.FileFormat.*
import com.joshtalks.joshskills.util.Utils.DATE_FORMAT
import java.io.*
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

enum class FileFormat(val extension: String) {
    VIDEO(".mp4"),
    AUDIO(".mp3"),
    TEXT(".txt"),
    IMAGE(".jpg"),
    PDF(".pdf"),
}

const val FILE_NAME_PREFIX = "JoshSkills_"
const val PARENT_DIRECTORY = "JoshSkills"

val fileOutputPath: String
    get() {
        val path = Environment.getExternalStorageDirectory()
            .toString() + File.separator + PARENT_DIRECTORY + File.separator
        val folder = File(path)
        if (!folder.exists())
            folder.mkdirs()
        return path
    }

fun writeIntoFile(context: Context, data: Intent, file: File?): File? {
    var videoAsset: AssetFileDescriptor? = null
    try {
        videoAsset = context.contentResolver.openAssetFileDescriptor(data.data!!, "r")
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }
    val fileInputStream: FileInputStream
    try {
        fileInputStream = videoAsset!!.createInputStream()
        val out = FileOutputStream(file)
        val buf = ByteArray(1024)
        var len: Int
        while (fileInputStream.read(buf).also { len = it } > 0) {
            out.write(buf, 0, len)
        }
        fileInputStream.close()
        out.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return file
}

@Throws(IOException::class)
fun copyFile(sourceFile: File?, destFile: File) {
    if (!destFile.parentFile.exists()) destFile.parentFile.mkdirs()
    if (!destFile.exists()) {
        destFile.createNewFile()
    }
    var source: FileChannel? = null
    var destination: FileChannel? = null
    try {
        source = FileInputStream(sourceFile).channel
        destination = FileOutputStream(destFile).channel
        destination.transferFrom(source, 0, source.size())
    } finally {
        source?.close()
        destination?.close()
    }
}


private fun getDirectory(fileFormat: FileFormat) =
    when (fileFormat) {
        VIDEO -> Environment.DIRECTORY_MOVIES
        AUDIO -> Environment.DIRECTORY_MUSIC
        IMAGE -> Environment.DIRECTORY_PICTURES
        TEXT, PDF -> Environment.DIRECTORY_DOCUMENTS
    }

fun getNewTempFile(context: Context, fileFormat: FileFormat): File {
    val timeStamp: String = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(
        Date()
    )
    val fileName: String = FILE_NAME_PREFIX + timeStamp + "_"
    val storageDir: File? = context.getExternalFilesDir(getDirectory(fileFormat))
    if (storageDir != null && storageDir.exists().not())
        storageDir.mkdirs()
    return File.createTempFile(fileName, fileFormat.extension, storageDir)
}

fun getFileExtension(filePath: String): String {
    return filePath.substring(filePath.lastIndexOf("."))
}