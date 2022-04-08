package com.joshtalks.joshskills.voip.notification

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.base.constants.INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.STARTING_POINT
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.model.IncomingCall

class ServiceNotification : NotificationData {
    override fun setTitle(): String {
        TODO("Not yet implemented")
    }

    override fun setContent(): String {
        TODO("Not yet implemented")
    }
}

class ConnectedNotification {
    fun notificationLayout(data: IncomingCall): RemoteViews {
        val remoteView = RemoteViews(Utils.context?.packageName, R.layout.call_notification)
        val destination="com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity"
        val callingActivity = Intent()
        val bundle = Bundle().apply {
            putString(STARTING_POINT, FROM_INCOMING_CALL)
            putInt(INCOMING_CALL_ID, data.getCallId())
        }
        callingActivity.apply {
            setClassName(Utils.context!!.applicationContext,destination)
            putExtras(bundle)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val acceptPendingIntent=
            PendingIntent.getActivity(Utils.context,1101,callingActivity, PendingIntent.FLAG_CANCEL_CURRENT)
        //val declinePendingIntent=PendingIntent.getActivity(Utils.context,1011,tapIntent,PendingIntent.FLAG_UPDATE_CURRENT)
        remoteView.setOnClickPendingIntent(R.id.answer_text,acceptPendingIntent)
        return remoteView
    }
}

