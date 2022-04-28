package com.joshtalks.joshskills.voip.mediator

import android.util.Log
import android.widget.RemoteViews
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.data.api.CallActionRequest
import com.joshtalks.joshskills.voip.data.api.ConnectionRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import com.joshtalks.joshskills.voip.getDeclineCallIntent
import com.joshtalks.joshskills.voip.openCallScreen
import com.joshtalks.joshskills.voip.voipLog

private const val TAG = "PeerToPeerCalling"
class PeerToPeerCalling : Calling {
    val voipNetwork = VoipNetwork.getVoipApi()

    override fun notificationLayout(data: IncomingCall): RemoteViews? {
        val remoteView = RemoteViews(Utils.context?.packageName, R.layout.call_notification)
        val acceptPendingIntent= openCallScreen()
        val declinePendingIntent= getDeclineCallIntent()
        remoteView.setOnClickPendingIntent(R.id.answer_text,acceptPendingIntent)
        remoteView.setOnClickPendingIntent(R.id.decline_text,declinePendingIntent)
        return remoteView
    }

    override suspend fun onPreCallConnect(callData: HashMap<String, Any>) {
        voipLog?.log("Calling API ---- $callData")
        if(callData.isIncomingCall()) {
            Log.d(TAG, "onPreCallConnect: INCOMING")
            val request = CallActionRequest(
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

    override suspend fun onCallDecline(callData: HashMap<String, Any>) {
        val request = CallActionRequest(
            callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
            mentorId = Utils.uuid,
            response = "DECLINE"
        )
        voipLog?.log("Calling API ---- $request")
        val response = voipNetwork.callAccept(request)
        if (response.isSuccessful)
            voipLog?.log("Sucessfull")
    }
}