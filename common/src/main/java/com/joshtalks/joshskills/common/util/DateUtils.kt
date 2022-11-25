package com.joshtalks.joshskills.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "DateUtils"

object DateUtils {
    fun getCurrentTimeStamp(pattern: String = "yyyy-MM-dd HH:mm:ss.SSS"): String {
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        System.currentTimeMillis()
        return dateFormat.format(Date())
    }
}


/*
fun main() {
    print(DateUtils.getCurrentTimeStamp())
}*/
