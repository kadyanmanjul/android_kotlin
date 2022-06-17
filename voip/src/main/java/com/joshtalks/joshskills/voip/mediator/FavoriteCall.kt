package com.joshtalks.joshskills.voip.mediator

import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.constant.REMOTE_USER_NAME
import com.joshtalks.joshskills.voip.data.api.CallActionRequest
import com.joshtalks.joshskills.voip.data.api.FavoriteCallActionRequest
import com.joshtalks.joshskills.voip.data.api.FavoriteConnectionRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork

private const val TAG = "FavoriteCall"

class FavoriteCall : CallCategory {
    val voipNetwork = VoipNetwork.getVoipApi()

    override fun notificationLayout(map: HashMap<String, String>): RemoteViews? {
        val remoteView =
            RemoteViews(Utils.context?.packageName, R.layout.favorite_call_notification)
        remoteView.setTextViewText(R.id.name, Utils.context?.getString(R.string.favorite_p2p_title))
        remoteView.setTextViewText(R.id.title, map[REMOTE_USER_NAME])
        val avatar: Bitmap? = getRandomName().textDrawableBitmap()
        remoteView.setImageViewBitmap(R.id.photo, avatar)
        val acceptPendingIntent = openCallScreen()
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
                response = 1
            )
            val response = voipNetwork.favouriteCallAccept(request)
            if (response.isSuccessful)
                Log.d(TAG, "onPreCallConnect: Successful")
        } else {
            Log.d(TAG, "onPreCallConnect: OUTGOING")
            val request = FavoriteConnectionRequest(
                mentorId = callData[INTENT_DATA_FPP_MENTOR_ID] as String,
            )
            val response = voipNetwork.startFavouriteCall(request)
            Log.d(TAG, "onPreCallConnect: $response")

        }
    }


    override suspend fun onCallDecline(callData: HashMap<String, Any>) {
        val request = FavoriteCallActionRequest(
            callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
            response = 0
        )
        Log.d(TAG, "onCallDecline: $request")
        val response = voipNetwork.favouriteCallReject(request)
        if (response.isSuccessful)
            Log.d(TAG, "onCallDecline: Successful")
    }
}