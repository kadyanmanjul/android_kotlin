package com.joshtalks.joshskills.ui.voip.analytics

import android.util.Log
import com.joshtalks.joshskills.ui.voip.RTC_CALLER_UID_KEY
import com.joshtalks.joshskills.ui.voip.RTC_CALL_ID
import com.joshtalks.joshskills.ui.voip.RTC_CHANNEL_KEY
import com.joshtalks.joshskills.ui.voip.RTC_UID_KEY

private const val TAG = "CurrentCallDetails"

object CurrentCallDetails {
    var channelName = ""
        private set
    var callerUid = ""
        private set
    var callieUid = ""
        private set
    var callId = ""
        private set

    fun reset() {
        Log.d(TAG, "reset: ")
        channelName = ""
        callerUid = ""
        callieUid = ""
        callId = ""
    }

    fun set(channelName: String, callerUid: String, callieUid: String, callId: String) {
        Log.d(
            TAG,
            "set: CN -> $channelName, CUID -> $callerUid, CEUID -> $callieUid, CID -> $callId"
        )
        this.channelName = channelName
        this.callerUid = callerUid
        this.callieUid = callieUid
        this.callId = if (callId.isEmpty()) this.callId else callId
    }

    fun fromMap(data: HashMap<String, String?>) {
        Log.d(TAG, "fromMap: ${data}")
        this.channelName = data[RTC_CHANNEL_KEY] ?: ""
        this.callieUid = data[RTC_UID_KEY] ?: ""
        this.callerUid = data[RTC_CALLER_UID_KEY] ?: ""
        this.callId =
            if (data[RTC_CALL_ID].isNullOrEmpty()) this.callId else data[RTC_CALL_ID] ?: ""
    }
}