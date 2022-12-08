package com.joshtalks.joshskills.voip.mediator

import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import com.joshtalks.joshskills.voip.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.voip.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.voip.base.constants.INTENT_DATA_PREVIOUS_CALL_ID
import com.joshtalks.joshskills.voip.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.data.api.CallActionRequest
import com.joshtalks.joshskills.voip.data.api.ConnectionRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import java.lang.Exception
import kotlin.collections.HashMap

private const val TAG = "PeerToPeerCalling"
class PeerToPeerCall : CallCategory {
    val voipNetwork = VoipNetwork.getVoipApi()

    override fun notificationLayout(map: HashMap<String, String>): RemoteViews {
        val remoteView = RemoteViews(Utils.context?.packageName, R.layout.call_notification_new)
        val avatar: Bitmap? = getRandomName().textDrawableBitmap()
        remoteView.setImageViewBitmap(R.id.photo, avatar)
        val acceptPendingIntent = getAcceptCallIntent()
        val declinePendingIntent = getDeclineCallIntent()
        remoteView.setOnClickPendingIntent(R.id.answer_text,acceptPendingIntent)
        remoteView.setOnClickPendingIntent(R.id.decline_text,declinePendingIntent)
        return remoteView
    }

    override suspend fun onPreCallConnect(callData: HashMap<String, Any>, direction: CallDirection) {
        Log.d(TAG, "Calling API ---- $callData")
        if (direction == CallDirection.INCOMING) {
            Log.d(TAG, "onPreCallConnect: INCOMING")
            val request = CallActionRequest(
                callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
                mentorId = Utils.uuid,
                response = "ACCEPT"
            )
            val response = voipNetwork.callAccept(request)
            if (response.isSuccessful)
                voipLog?.log("Sucessfull")
        } else {
            Log.d(TAG, "onPreCallConnect: OUTGOING")
            val request = ConnectionRequest(
                topicId = (callData[INTENT_DATA_TOPIC_ID] as String).toInt(),
                mentorId = Utils.uuid,
                courseId = (callData[INTENT_DATA_COURSE_ID] as String).toInt(),
                oldCallId = (callData[INTENT_DATA_PREVIOUS_CALL_ID] as? Int)?.toInt(),
            )
            val response = voipNetwork.startPeerToPeerCall(request)
            Log.d(TAG, "onPreCallConnect: $response")
        }
    }

    override suspend fun onCallDecline(callData: HashMap<String, Any>) {
        val request = CallActionRequest(
            callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
            mentorId = Utils.uuid,
            response = "DECLINE"
        )
        Log.d(TAG, "onCallDecline: $request")
        val response = voipNetwork.callAccept(request)
        if (response.isSuccessful)
            Log.d(TAG, "onCallDecline: Successful")
    }
}

class UserBlockedException() : Exception()