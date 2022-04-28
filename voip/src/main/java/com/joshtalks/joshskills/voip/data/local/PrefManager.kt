package com.joshtalks.joshskills.voip.data.local

import android.content.Context
import android.content.SharedPreferences
import com.joshtalks.joshskills.base.constants.PREF_KEY_WEBRTC_CURRENT_STATE
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.constant.IDLE
import kotlinx.coroutines.*

const val LATEST_PUBNUB_MESSAGE_TIME = "josh_pref_key_latest_pubnub_message_time"
const val VOIP_STATE = "josh_pref_key_voip_state"
const val INCOMING_CALL = "josh_pref_key_incoming_call"

class PrefManager {
    companion object {
        lateinit var preferenceManager: SharedPreferences
        val coroutineExceptionHandler = CoroutineExceptionHandler{_ , e ->
            e.printStackTrace()
        }
        val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

        @Synchronized
        fun initServicePref(context: Context) {
            preferenceManager = context.getSharedPreferences(
                context.getString(R.string.voip_service_shared_pref_file_name),
                Context.MODE_PRIVATE
            )
        }

        fun getLatestPubnubMessageTime() : Long {
            return preferenceManager.getLong(LATEST_PUBNUB_MESSAGE_TIME, 0L)
        }

        fun setLatestPubnubMessageTime(timetoken : Long) {
            val editor = preferenceManager.edit()
            editor.putLong(LATEST_PUBNUB_MESSAGE_TIME, timetoken)
            editor.apply()
        }

        fun getVoipState() : Int {
            return preferenceManager.getInt(VOIP_STATE, IDLE)
        }

        fun setVoipState(state : Int) {
            val editor = preferenceManager.edit()
            editor.putInt(VOIP_STATE, state)
            editor.commit()
        }

        fun getIncomingCallId() : Int {
            return preferenceManager.getInt(INCOMING_CALL, -1)
        }

        fun setIncomingCallId(callId : Int) {
            val editor = preferenceManager.edit()
            editor.putInt(INCOMING_CALL, callId)
            editor.commit()
        }
    }
}