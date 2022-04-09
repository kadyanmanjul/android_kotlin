package com.joshtalks.joshskills.voip

import android.app.Application
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.joshtalks.joshskills.base.constants.CALL_DISCONNECTED_URI
import com.joshtalks.joshskills.base.constants.CALL_ID
import com.joshtalks.joshskills.base.constants.CALL_START_TIME
import com.joshtalks.joshskills.base.constants.CALL_TYPE
import com.joshtalks.joshskills.base.constants.CHANNEL_NAME
import com.joshtalks.joshskills.base.constants.CONTENT_URI
import com.joshtalks.joshskills.base.constants.CURRENT_HOLD_STATE_URI
import com.joshtalks.joshskills.base.constants.CURRENT_MUTE_STATE_URI
import com.joshtalks.joshskills.base.constants.CURRENT_REMOTE_MUTE_STATE_URI
import com.joshtalks.joshskills.base.constants.CURRENT_SPEAKER_STATE_URI
import com.joshtalks.joshskills.base.constants.CURRENT_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.INCOMING_CALL_URI
import com.joshtalks.joshskills.base.constants.IS_MUTE
import com.joshtalks.joshskills.base.constants.IS_ON_HOLD
import com.joshtalks.joshskills.base.constants.IS_REMOTE_USER_MUTE
import com.joshtalks.joshskills.base.constants.IS_SPEAKER_ON
import com.joshtalks.joshskills.base.constants.REMOTE_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.REMOTE_USER_IMAGE
import com.joshtalks.joshskills.base.constants.REMOTE_USER_NAME
import com.joshtalks.joshskills.base.constants.RESET_CURRENT_CALL_STATE_URI
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_DISCONNECT_CALL
import com.joshtalks.joshskills.base.constants.START_CALL_TIME_COLUMN
import com.joshtalks.joshskills.base.constants.START_CALL_TIME_URI
import com.joshtalks.joshskills.base.constants.TOPIC_NAME
import com.joshtalks.joshskills.base.constants.VOIP_STATE
import com.joshtalks.joshskills.base.constants.VOIP_STATE_URI
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.voip.calldetails.IncomingCallData
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.log.JoshLog

// TODO: Must Refactor
val voipLog = JoshLog.getInstanceIfEnable(com.joshtalks.joshskills.voip.log.Feature.VOIP)

//fun Context.updateUserMuteState(state: Boolean, ) {
//    voipLog?.log("updateUserMuteState --> $state")
//    val values = ContentValues(1).apply {
//        put(IS_MUTE, state)
//    }
//    val data = contentResolver.insert(
//        Uri.parse(CONTENT_URI + CURRENT_MUTE_STATE_URI),
//        values
//    )
//    voipLog?.log("Data --> $data")
//}

//fun Context.updateUserSpeakerState(state: Boolean, ) {
//    voipLog?.log("updateUserMuteState --> $state")
//    val values = ContentValues(1).apply {
//        put(IS_SPEAKER_ON, state)
//    }
//    val data = contentResolver.insert(
//        Uri.parse(CONTENT_URI + CURRENT_SPEAKER_STATE_URI),
//        values
//    )
//    voipLog?.log("Data --> $data")
//}

fun Context.updateUserHoldState(state: Boolean, ) {
    voipLog?.log("updateUserMuteState --> $state")
    val values = ContentValues(1).apply {
        put(IS_ON_HOLD, state)
    }
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + CURRENT_HOLD_STATE_URI),
        values
    )
    voipLog?.log("Data --> $data")
}

fun Context.updateRemoteUserMuteState(state: Boolean, ) {
    voipLog?.log("updateUserMuteState --> $state")
    val values = ContentValues(1).apply {
        put(IS_REMOTE_USER_MUTE, state)
    }
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + CURRENT_REMOTE_MUTE_STATE_URI),
        values
    )
    voipLog?.log("Data --> $data")
}

fun Context.updateStartCallTime(
    timestamp: Long,
    remoteUserName: String = "",
    remoteUserImage: String? = null,
    callId: Int = -1,
    callType: Int = -1,
    remoteUserAgoraId: Int = -1,
    currentUserAgoraId: Int = -1,
    channelName: String = "",
    topicName: String = ""
) {
    voipLog?.log("QUERY")
    val values = ContentValues(9).apply {
        put(CALL_START_TIME, timestamp)
        put(REMOTE_USER_NAME, remoteUserName)
        put(REMOTE_USER_IMAGE, remoteUserImage)
        put(REMOTE_USER_AGORA_ID, remoteUserAgoraId)
        put(CALL_ID, callId)
        put(CALL_TYPE, callType)
        put(CHANNEL_NAME, channelName)
        put(TOPIC_NAME, topicName)
        put(CURRENT_USER_AGORA_ID, currentUserAgoraId)
    }
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + START_CALL_TIME_URI),
        values
    )
    voipLog?.log("Data --> $data")
}

fun Context.getStartCallTime(): Long {
    val startCallTimeCursor = contentResolver.query(
        Uri.parse(CONTENT_URI + START_CALL_TIME_URI),
        null,
        null,
        null,
        null
    )
    startCallTimeCursor?.moveToFirst()
    val startTime = startCallTimeCursor?.getLong(
        startCallTimeCursor.getColumnIndex(
            START_CALL_TIME_COLUMN
        )
    )
    startCallTimeCursor?.close()
    return startTime ?: 0L
}

fun Context.updateLastCallDetails() {
    voipLog?.log("QUERY")
    val values = ContentValues(1).apply {
        put(CALL_START_TIME, 0L)
    }
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + CALL_DISCONNECTED_URI),
        values
    )
    voipLog?.log("Data --> $data")
}

fun Context.resetCallUIState() {
    voipLog?.log("QUERY")
    val values = ContentValues(1)
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + RESET_CURRENT_CALL_STATE_URI),
        values
    )
}

fun Context.updateIncomingCallDetails() {
    voipLog?.log("QUERY")
    val values = ContentValues(2).apply {
        put(CALL_ID, IncomingCallData.callId)
        put(CALL_TYPE, IncomingCallData.callType)
    }
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + INCOMING_CALL_URI),
        values
    )
    voipLog?.log("Data --> $data")
}

fun Context.updateVoipState(state: Int) {
    voipLog?.log("Setting Voip State --> $state")
    val values = ContentValues(1).apply {
        put(VOIP_STATE, state)
    }
    val data = contentResolver.insert(
        Uri.parse(CONTENT_URI + VOIP_STATE_URI),
        values
    )
    voipLog?.log("Data --> $data")
}

fun openCallScreen(): PendingIntent {
    val destination = "com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity"
    val intent = Intent()
    intent.apply {
        setClassName(Utils.context!!.applicationContext, destination)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        Utils.context,
        1102,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT
    )
}

fun Context.getHangUpIntent(): PendingIntent {
    val intent = Intent(this, CallingRemoteService::class.java).apply {
        action = SERVICE_ACTION_DISCONNECT_CALL
    }

    return PendingIntent.getService(
        Utils.context,
        1103,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT
    )
}

class Utils {
    companion object {
        var context : Application? = null
       var apiHeader : ApiHeader? = null
        var uuid : String? = null

        fun initUtils(application: Application ) {
            this.context = application
        }
    }
}