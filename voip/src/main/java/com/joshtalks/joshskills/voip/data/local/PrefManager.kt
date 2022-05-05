package com.joshtalks.joshskills.voip.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.joshtalks.joshskills.base.constants.PREF_KEY_LOCAL_USER_AGORA_ID
import com.joshtalks.joshskills.voip.R
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.voip.constant.PREF_KEY_PSTN_STATE
import com.joshtalks.joshskills.voip.constant.PSTN_STATE_IDLE
import com.joshtalks.joshskills.voip.constant.State
import kotlinx.coroutines.*

const val LATEST_PUBNUB_MESSAGE_TIME = "josh_pref_key_latest_pubnub_message_time"
const val VOIP_STATE = "josh_pref_key_voip_state"
const val INCOMING_CALL = "josh_pref_key_incoming_call"
const val LOCAL_USER_AGORA_ID = "josh_pref_key_local_user_agora_id"

private const val TAG = "PrefManager"

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

        fun getVoipState() : State {
            val ordinal = preferenceManager.getInt(VOIP_STATE, State.IDLE.ordinal)
            Log.d(TAG, "getVoipState: $ordinal")
            Log.d(TAG, "getVoipState: ${State.values()}")
            return State.values()[ordinal]
        }

        fun setVoipState(state : State) {
            Log.d(TAG, "Setting Voip State : $state")
            Log.d(TAG, "Setting Voip State : ${state.ordinal}")
            val editor = preferenceManager.edit()
            editor.putInt(VOIP_STATE, state.ordinal)
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

        fun getLocalUserAgoraId() : Int {
            return preferenceManager.getInt(LOCAL_USER_AGORA_ID, -1)
        }

        fun setLocalUserAgoraId(localUserAgoraId : Int) {
            val editor = preferenceManager.edit()
            editor.putInt(LOCAL_USER_AGORA_ID, localUserAgoraId)
            editor.commit()
        }

        fun savePstnState(state: String) {
            Log.d(TAG, "Setting pstn State : $state")
            val editor = preferenceManager.edit()
                editor.putString(PREF_KEY_PSTN_STATE, state)
               editor.commit()
        }
        fun getPstnState() : String {
            Log.d(TAG, "Getting pstn State")
            return preferenceManager.getString(PREF_KEY_PSTN_STATE, PSTN_STATE_IDLE).toString()
        }
    }
}