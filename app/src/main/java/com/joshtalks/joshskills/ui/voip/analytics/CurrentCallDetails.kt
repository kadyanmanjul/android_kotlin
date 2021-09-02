package com.joshtalks.joshskills.ui.voip.analytics

import android.util.Log
import com.joshtalks.joshskills.ui.voip.RTC_CALLER_UID_KEY
import com.joshtalks.joshskills.ui.voip.RTC_CALL_ID
import com.joshtalks.joshskills.ui.voip.RTC_CHANNEL_KEY
import com.joshtalks.joshskills.ui.voip.RTC_UID_KEY
import java.util.concurrent.atomic.AtomicBoolean

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
    @Volatile
    var isSpeakingPushed = AtomicBoolean(false)
        private set
    @Volatile
    var isListeningPushed = AtomicBoolean(false)
        private set
    @Volatile
    var isCallConnectedScreenVisible = AtomicBoolean(false)
        private set
    @Volatile
    var isOnHold = AtomicBoolean(false)
        private set

    fun reset() {
        Log.d(TAG, "reset: ")
        channelName = ""
        callerUid = ""
        callieUid = ""
        callId = ""
        this.isSpeakingPushed = AtomicBoolean(false)
        this.isListeningPushed = AtomicBoolean(false)
        this.isCallConnectedScreenVisible = AtomicBoolean(false)
        this.isOnHold = AtomicBoolean(false)
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
        this.isSpeakingPushed = AtomicBoolean(false)
        this.isListeningPushed = AtomicBoolean(false)
        this.isCallConnectedScreenVisible = AtomicBoolean(false)
        this.isOnHold = AtomicBoolean(false)
    }

    fun fromMap(data: HashMap<String, String?>) {
        Log.d(TAG, "fromMap: ${data}")
        this.channelName = data[RTC_CHANNEL_KEY] ?: ""
        this.callieUid = data[RTC_UID_KEY] ?: ""
        this.callerUid = data[RTC_CALLER_UID_KEY] ?: ""
        this.callId =
            if (data[RTC_CALL_ID].isNullOrEmpty()) this.callId else data[RTC_CALL_ID] ?: ""
        this.isSpeakingPushed = AtomicBoolean(false)
        this.isListeningPushed = AtomicBoolean(false)
        this.isCallConnectedScreenVisible = AtomicBoolean(false)
        this.isOnHold = AtomicBoolean(false)
    }

    @Synchronized fun speakingPushed() {
        this.isSpeakingPushed = AtomicBoolean(true)
    }

    @Synchronized fun listeningPushed() {
        this.isListeningPushed = AtomicBoolean(true)
    }

    @Synchronized fun callConnectedScreenVisible() {
        this.isCallConnectedScreenVisible = AtomicBoolean(true)
    }

    @Synchronized fun callOnHold() {
        this.isOnHold = AtomicBoolean(true)
    }

    @Synchronized fun callResumed() {
        this.isOnHold = AtomicBoolean(false)
    }

    fun state() : State {
        return State(channelName = this.channelName,
            callerUid =  this.callerUid,
            callieUid = this.callieUid,
            callId =  this.callId,
            isListeningPushed = this.isListeningPushed.get(),
            isSpeakingPushed  = this.isSpeakingPushed.get(),
            isCallConnectedScreenVisible = this.isCallConnectedScreenVisible.get(),
            isOnHold = this.isOnHold.get()
        )
    }

    data class State(val channelName : String,
                     val callerUid : String,
                     val callieUid: String,
                     val callId : String,
                     val isListeningPushed : Boolean,
                     val isSpeakingPushed : Boolean,
                     val isCallConnectedScreenVisible : Boolean,
                     val isOnHold : Boolean)
}