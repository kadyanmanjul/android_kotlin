package com.joshtalks.joshskills.voip.mediator

import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import com.joshtalks.joshskills.base.constants.INTENT_DATA_GROUP_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.data.api.*

private const val TAG = "GroupP2PCalling"

class GroupCall : CallCategory {

    val voipNetwork = VoipNetwork.getVoipApi()

    override fun notificationLayout(map: HashMap<String, String>): RemoteViews? {
        val remoteView = RemoteViews(Utils.context?.packageName, R.layout.call_group_notification)
        val avatar: Bitmap? = getRandomName().textDrawableBitmap()
        remoteView.setImageViewBitmap(R.id.photo, avatar)
        val acceptPendingIntent = openCallScreen()
        val declinePendingIntent = getDeclineCallIntent()
        remoteView.setOnClickPendingIntent(R.id.answer_text, acceptPendingIntent)
        remoteView.setOnClickPendingIntent(R.id.decline_text, declinePendingIntent)
        return remoteView
    }

    override suspend fun onPreCallConnect(callData: HashMap<String, Any>, direction: CallDirection) {
        Log.d(TAG, "Calling API ---- $callData")
        if (direction == CallDirection.INCOMING) {
            Log.d(TAG, "onPreCallConnect: INCOMING GROUP")
            val request = GroupCallActionRequest(
                callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
                response = "ACCEPT"
            )
            val response = voipNetwork.groupCallAccept(request)
            if (response.isSuccessful)
                voipLog?.log("Successful")
        } else {
            Log.d(TAG, "onPreCallConnect: OUTGOING")
            val request = GroupConnectionRequest(
                topicId = (callData[INTENT_DATA_TOPIC_ID] as String).toInt(),
                groupId = (callData[INTENT_DATA_GROUP_ID] as String)
            )
            val response = voipNetwork.startGroupCall(request)
            Log.d(TAG, "onPreCallConnect: $response")
        }
    }

    override suspend fun onCallDecline(callData: HashMap<String, Any>) {
        val request = GroupCallActionRequest(
            callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
            response = "DECLINE"
        )
        Log.d(TAG, "onCallDecline: $request")
        val response = voipNetwork.groupCallReject(request)
        if (response.isSuccessful)
            Log.d(TAG, "onCallDecline: Successful")
    }
}