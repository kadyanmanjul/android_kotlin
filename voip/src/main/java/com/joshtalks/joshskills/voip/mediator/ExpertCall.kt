package com.joshtalks.joshskills.voip.mediator

import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.constant.IS_PREMIUM_USER
import com.joshtalks.joshskills.voip.constant.REMOTE_USER_NAME
import com.joshtalks.joshskills.voip.constant.TOAST_MESSAGE
import com.joshtalks.joshskills.voip.data.api.ExpertConnectionRequest
import com.joshtalks.joshskills.voip.data.api.FavoriteCallActionRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork

private const val TAG = "ExpertCall"

class ExpertCall : CallCategory {
    val voipNetwork = VoipNetwork.getVoipApi()

    override fun notificationLayout(map: HashMap<String, String>): RemoteViews? {
        val remoteView = RemoteViews(Utils.context?.packageName, R.layout.favorite_call_notification)
        remoteView.setTextViewText(R.id.title, map[REMOTE_USER_NAME])
        if (map[IS_PREMIUM_USER].equals("true")) {
            remoteView.setTextViewText(R.id.name, Utils.context?.getString(R.string.premium_p2p_title))
            remoteView.setImageViewResource(R.id.photo, R.drawable.premium_user_banner)
        } else {
            remoteView.setTextViewText(R.id.name, Utils.context?.getString(R.string.expert_p2p_title))
            val avatar: Bitmap? = getRandomName().textDrawableBitmap()
            remoteView.setImageViewBitmap(R.id.photo, avatar)
        }
        val acceptPendingIntent = openFavoriteCallScreen()
        val declinePendingIntent = getDeclineCallIntent()
        remoteView.setOnClickPendingIntent(R.id.answer_text, acceptPendingIntent)
        remoteView.setOnClickPendingIntent(R.id.decline_text, declinePendingIntent)
        return remoteView
    }

    override suspend fun onPreCallConnect(
        callData: HashMap<String, Any>,
        direction: CallDirection,
    ) {
        Log.d(TAG, "Calling API ---- $callData")
        if (direction == CallDirection.INCOMING) {
            Log.d(TAG, "onPreCallConnect: FPP INCOMING")
            val request = FavoriteCallActionRequest(
                callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
                response = "ACCEPT"
            )
            val response = voipNetwork.favouriteCallAccept(request)
            if (response.isSuccessful)
                Log.d(TAG, "onPreCallConnect: Successful")
        } else {
            var response: HashMap<String, Any?>? = null
            Log.d("sagar", "onPreCallConnect: OUTGOING ${callData[IS_EXPERT_CALLING]}")

            val request = ExpertConnectionRequest(
                mentorId = callData[INTENT_DATA_FPP_MENTOR_ID] as String,
                courseId = Utils.courseId?.toInt(),
                mentorName = callData[INTENT_DATA_FPP_NAME] as String,
                isPremiumUser = callData[INTENT_DATA_EXPERT_PREMIUM] as Boolean
            )
            response = voipNetwork.startExpertCall(request)
            Log.d(TAG, "onPreCallConnect:  Expert$response")

            if (response[TOAST_MESSAGE] != null && response[TOAST_MESSAGE]?.equals("") != true) {
                response[TOAST_MESSAGE]?.let { showToast(it.toString()) }
            }
        }
    }

    override suspend fun onCallDecline(callData: HashMap<String, Any>) {
        val request = FavoriteCallActionRequest(
            callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
            response = "DECLINE"
        )
        Log.d(TAG, "onCallDecline: $request")
        val response = voipNetwork.favouriteCallReject(request)
        if (response.isSuccessful)
            Log.d(TAG, "onCallDecline: Successful")
    }
}