package com.joshtalks.joshskills.voip.mediator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.widget.RemoteViews
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_INCOMING_CALL_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.voip.*
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.data.api.CallAcceptRequest
import com.joshtalks.joshskills.voip.data.api.ConnectionRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import com.joshtalks.joshskills.voip.notification.VoipNotification
import java.util.*
import kotlin.collections.HashMap

private const val TAG = "PeerToPeerCalling"
class PeerToPeerCalling : Calling {
    val voipNetwork = VoipNetwork.getVoipApi()

    override fun notificationLayout(data: IncomingCall): RemoteViews {
        val remoteView = RemoteViews(Utils.context?.packageName, R.layout.call_notification_new)
        val avatar: Bitmap? = getRandomName().textDrawableBitmap()
        remoteView.setImageViewBitmap(R.id.photo, avatar)
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

    override suspend fun onCallDecline(callData: HashMap<String, Any>) {
        val request = CallAcceptRequest(
            callId = callData[INTENT_DATA_INCOMING_CALL_ID] as Int,
            mentorId = Utils.uuid,
            response = "DECLINE"
        )
        voipLog?.log("Calling API ---- $request")
        val response = voipNetwork.callAccept(request)
        if (response.isSuccessful)
            voipLog?.log("Sucessfull")
    }
    private fun getRandomName(): String {
        val name = "ABCDFGHIJKLMNOPRSTUVZ"
        val ename = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return name.random().toString().plus(ename.random().toString())
    }
    private fun String.textDrawableBitmap(
        width: Int = 48,
        height: Int = 48,
        bgColor: Int = -1
    ): Bitmap? {
        val rnd = Random()
        val color = if (bgColor == -1)
            Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
        else
            bgColor

        val font = Typeface.createFromAsset(
            Utils.context?.assets,
            "fonts/OpenSans-SemiBold.ttf"
        )
        val drawable = TextDrawable.builder()
            .beginConfig()
            .textColor(Color.WHITE)
            .fontSize(20)
            .useFont(font)
            .toUpperCase()
            .endConfig()
            .buildRound(this, color)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}