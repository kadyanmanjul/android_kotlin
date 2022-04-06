package com.joshtalks.joshskills.voip.mediator

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.widget.RemoteViews
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.data.api.ConnectionRequest

const val ACCEPT_REQUEST_CODE = 6943

interface Calling {
    fun notificationLayout() : RemoteViews {
        val remoteView = RemoteViews(Utils.context?.packageName,R.layout.call_notification)
        val destination="com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity"
        val callingActivity = Intent()
        callingActivity.apply {
            setClassName(Utils.context!!.applicationContext,destination)
            putExtra("openCallFragment",true)
        }
        val acceptPendingIntent=PendingIntent.getActivity(Utils.context,1011,callingActivity,PendingIntent.FLAG_UPDATE_CURRENT)
//                        val declinePendingIntent=PendingIntent.getActivity(Utils.context,1011,tapIntent,PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.answer_text,acceptPendingIntent)
        return remoteView
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