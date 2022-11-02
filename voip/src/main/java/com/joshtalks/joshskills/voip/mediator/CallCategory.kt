package com.joshtalks.joshskills.voip.mediator

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.joshtalks.joshskills.voip.Utils

const val ACCEPT_REQUEST_CODE = 6943
enum class CallDirection {
    INCOMING,
    OUTGOING
}

interface CallCategory {
    fun notificationLayout(map: HashMap<String, String>) : RemoteViews? {
        return null
    }

    suspend fun onPreCallConnect(callData: HashMap<String, Any>, direction: CallDirection) {
        // API Call
    }

    suspend fun onCallDecline(callData: HashMap<String, Any>) {
        // Report and Block
    }

    fun notificationTitle() : String {
        return "Josh Talks"
    }

    fun notificationContent() : String {
        return "Calling"
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun acceptAction() : PendingIntent {
        val callingActivity = Intent("com.joshtalks.joshskills.voip.CallActivity")
        return PendingIntent.getActivity(
            Utils.context,
            ACCEPT_REQUEST_CODE,
            callingActivity,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun onNotificationClick() : String {
        return "com.joshtalks.joshskills.voip.CallActivity"
    }
}