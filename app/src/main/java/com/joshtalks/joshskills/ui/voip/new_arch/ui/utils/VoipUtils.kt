package com.joshtalks.joshskills.ui.voip.new_arch.ui.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.voip.constant.CONTENT_VOIP_STATE_AUTHORITY
import com.joshtalks.joshskills.voip.constant.CURRENT_VOIP_STATE
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.constant.VOIP_STATE_PATH

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


