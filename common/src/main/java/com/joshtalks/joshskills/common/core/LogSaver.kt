package com.joshtalks.joshskills.common.core

import android.annotation.SuppressLint
import android.os.Environment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * LogSaver is a object to save logs
 * Use LogSaver.startSavingLog() to generate file of logs in external directory
 * example directory to see logs (might be different for different devices)->
 * /storage/emulated/0/Android/data/com.joshtalks.joshskills/files/logs
 */
object LogSaver {

    private var ifWritable: Boolean = false
    private var copyLogExec: Process? = null

    init {
        ifWritable = isExternalStorageWritable()
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    @SuppressLint("SimpleDateFormat")
     fun startSavingLog() {
        if (ifWritable) {
            AppObjectController.joshApplication.applicationContext.getExternalFilesDir(null)
                ?.let { publicAppDirectory ->
                    val logDirectory = File("${publicAppDirectory.absolutePath}/logs")
                    if (!logDirectory.exists()) {
                        logDirectory.mkdir()
                    }

                    val logFile = File(
                        logDirectory,
                        "logcat_" + SimpleDateFormat("dd-M-yyyy_HH:mm:ss").format(Date()) + ".txt"
                    )
                        try {
                            stopSavingLog()
                            Runtime.getRuntime().exec("logcat -c")
                            copyLogExec = Runtime.getRuntime().exec("logcat -f $logFile")
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                }
        }
    }

     fun stopSavingLog() {
        copyLogExec?.destroy()
    }
}