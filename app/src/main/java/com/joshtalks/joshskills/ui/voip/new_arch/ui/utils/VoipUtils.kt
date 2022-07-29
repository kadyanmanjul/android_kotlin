package com.joshtalks.joshskills.ui.voip.new_arch.ui.utils

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.voip.constant.CONTENT_VOIP_STATE_AUTHORITY
import com.joshtalks.joshskills.voip.constant.CURRENT_VOIP_STATE
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.constant.VOIP_STATE_PATH
import java.util.*


private const val TAG = "Utils"

class VoipUtils {
    companion object {
        var context : Application? = null

        fun initUtils(application: Application ) {
            this.context = application
        }
    }
}

private fun Cursor?.getStringData(columnName : String) : String {
    return this?.getString(this.getColumnIndex(columnName)) ?: throw NoSuchElementException("$columnName is NULL from cursor")
}


fun Context.getVoipState(): State {
    val stateCursor = contentResolver.query(
        Uri.parse(CONTENT_VOIP_STATE_AUTHORITY + VOIP_STATE_PATH),
        null,
        null,
        null,
        null
    )
    stateCursor?.moveToFirst()
    val state = stateCursor.getStringData(CURRENT_VOIP_STATE)
    stateCursor?.close()
    return State.values()[state.toInt()]
}


fun Context.ifEnoughMemorySize(): Boolean {
    val activityManager: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo: ActivityManager.MemoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val systemTotal: Long = memoryInfo.totalMem
    val systemFree: Long = memoryInfo.availMem
    val systemUsed = systemTotal - systemFree

    Log.d(TAG, "getMemorySize: $systemTotal $systemFree  $systemUsed ${bytesToHuman(systemFree)} ${memoryInfo.lowMemory}")

    return bytesToHuman(systemFree).toDouble() > 700 && !memoryInfo.lowMemory
}


private fun floatForm(d: Double): String {
    return String.format(Locale.US, "%.2f", d)
}

private fun bytesToHuman(size: Long): String {
    val Kb: Long = 1024
    val Mb = Kb * 1024
    return floatForm(size.toDouble() / Mb)
}


