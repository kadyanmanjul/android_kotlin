package com.joshtalks.joshskills.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
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
private const val EOF = -1
private const val DEFAULT_BUFFER_SIZE = 1024 * 4

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
        FileFormat.VIDEO -> Environment.DIRECTORY_MOVIES
        FileFormat.AUDIO -> Environment.DIRECTORY_MUSIC
        FileFormat.IMAGE -> Environment.DIRECTORY_PICTURES
        FileFormat.TEXT, FileFormat.PDF -> Environment.DIRECTORY_DOCUMENTS
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

@Throws(IOException::class)
fun uriToFile(context: Context, uri: Uri?): File {
    val inputStream = context.contentResolver.openInputStream(uri!!)
    val tempFile = getNewTempFile(context, FileFormat.VIDEO)
    tempFile.deleteOnExit()
    try {
        val out = FileOutputStream(tempFile)
        if (inputStream != null) {
            copy(inputStream, out)
            inputStream.close()
            out.close()
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }
    return tempFile
}

@Throws(IOException::class)
private fun copy(input: InputStream, output: OutputStream): Long {
    var count: Long = 0
    var n: Int
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (EOF !== input.read(buffer).also { n = it }) {
        output.write(buffer, 0, n)
        count += n.toLong()
    }
    return count
}

fun getBitMapFromView(view: View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}

fun Bitmap.toFile(context: Context): File {
    val file = getNewTempFile(context, FileFormat.IMAGE)
    try {
        val out = FileOutputStream(file)
        this.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.close()
        return file
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return file
}