package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.utils

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.database.Cursor
import android.util.Log
import com.joshtalks.joshskills.common.core.BLOCK_STATUS
import com.joshtalks.joshskills.common.core.FT_CALLS_LEFT
import com.joshtalks.joshskills.common.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.common.ui.lesson.speaking.spf_models.BlockStatusModel
import com.joshtalks.joshskills.common.voip.constant.State
import com.joshtalks.joshskills.common.voip.data.local.PrefManager
import com.joshtalks.joshskills.common.core.PrefManager as CorePrefManager
import java.time.Duration
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


fun getVoipState(): State {
    val state = PrefManager.getVoipState().ordinal
    return State.values()[state]
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

fun isBlocked(): Boolean {
    val blockStatus = CorePrefManager.getBlockStatusObject(BLOCK_STATUS)
    if (CorePrefManager.getIntValue(FT_CALLS_LEFT, defValue = 15) == 0 &&
        CorePrefManager.getBoolValue(IS_FREE_TRIAL))
        return true
    if (blockStatus?.timestamp?.toInt() == 0)
        return false

    if (checkWithinBlockTimer(blockStatus))
        return true
    return false
}

private fun checkWithinBlockTimer(blockStatus: BlockStatusModel?): Boolean {
    if (blockStatus != null) {
        val durationInMillis = Duration.ofMinutes(blockStatus.duration.toLong()).toMillis()
        val unblockTimestamp = blockStatus.timestamp + durationInMillis
        if (System.currentTimeMillis() <= unblockTimestamp) {
            return true
        }
        if (blockStatus.callsLeft == 0)
            return true
    }
    return false
}

