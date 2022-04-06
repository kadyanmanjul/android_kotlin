package com.joshtalks.joshskills.voip.mediator

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.joshtalks.joshskills.base.constants.INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.base.constants.STARTING_POINT
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.data.api.CallAcceptRequest
import com.joshtalks.joshskills.voip.data.api.ConnectionRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import com.joshtalks.joshskills.voip.voipLog

private const val TAG = "PeerToPeerCalling"
class PeerToPeerCalling : Calling {
    val voipNetwork = VoipNetwork.getVoipApi()

    override fun notificationLayout(data: IncomingCall): RemoteViews? {
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

    override suspend fun onPreCallConnect(callData: HashMap<String, Any>) {
        voipLog?.log("Calling API ---- $callData")
        if(callData.isIncomingCall()) {
            Log.d(TAG, "onPreCallConnect: INCOMING")
            val request = CallAcceptRequest(
                callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
                mentorId = Utils.uuid,
                response = "ACCEPT"
            )
            voipLog?.log("Calling API ---- $request")
            val response = voipNetwork.callAccept(request)
            if (response.isSuccessful)
                voipLog?.log("Sucessfull")
        } else {
            Log.d(TAG, "onPreCallConnect: OUTGOING")
            val request = ConnectionRequest(
                topicId = (callData[INTENT_DATA_TOPIC_ID] as String).toInt(),
                mentorId = Utils.uuid,
                courseId = (callData[INTENT_DATA_COURSE_ID] as String).toInt()
            )
            voipLog?.log("Calling API ---- $request")
            val response = voipNetwork.setUpConnection(request)
            if (response.isSuccessful)
                voipLog?.log("Sucessfull")
        }
    }

    private fun HashMap<String, Any>.isIncomingCall() : Boolean {
        return get(INTENT_DATA_INCOMING_CALL_ID) != null
    }

    override fun onCallDisconnect() {
        TODO("Not yet implemented")
    }
}