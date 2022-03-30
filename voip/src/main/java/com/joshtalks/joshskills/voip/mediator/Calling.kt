package com.joshtalks.joshskills.voip.mediator

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.data.api.ConnectionRequest

const val ACCEPT_REQUEST_CODE = 6943

interface Calling {
    fun notificationLayout() : Int? {
        return null
    }

    suspend fun onPreCallConnect(callData: HashMap<String, Any>) {
        // API Call
    }

    fun onCallDisconnect() {
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
        return PendingIntent.getActivity(Utils.context, ACCEPT_REQUEST_CODE, callingActivity, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun onNotificationClick() : String {
        return "com.joshtalks.joshskills.voip.CallActivity"
    }
}