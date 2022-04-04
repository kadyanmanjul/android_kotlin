package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.base.constants.PREF_KEY_CURRENT_CALL_ID
import com.joshtalks.joshskills.base.constants.PREF_KEY_CURRENT_CALL_START_TIME
import com.joshtalks.joshskills.base.constants.PREF_KEY_CURRENT_CALL_TYPE
import com.joshtalks.joshskills.base.constants.PREF_KEY_CURRENT_REMOTE_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.PREF_KEY_CURRENT_REMOTE_USER_IMAGE
import com.joshtalks.joshskills.base.constants.PREF_KEY_CURRENT_REMOTE_USER_NAME
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_CALL_ID
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_CALL_START_TIME
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_CALL_TYPE
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_REMOTE_USER_AGORA_ID
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_REMOTE_USER_IMAGE
import com.joshtalks.joshskills.base.constants.PREF_KEY_LAST_REMOTE_USER_NAME
import com.joshtalks.joshskills.base.constants.PREF_KEY_WEBRTC_CURRENT_STATE
import com.joshtalks.joshskills.voip.constant.IDLE

/**
 * Require DataBinding in targeted xml with following instruction ->
 * 1. import @View
 * 2. add variable @callBar of type CallBar class
 * 3. add @CallBarLayout in xml with required attributes
 * 4. use binding adapters setters @onCallBarClick (callBar::intentToCallActivity)
 * 5. toggle visibility using @isCallONGoing ObservableBoolean
 *  Required to set Variable @callBar in Activity as "binding.callBar= CallBar(this)"
 */

private const val TAG = "CallBar"

class VoipPref {

    companion object {
        lateinit var preferenceManager: SharedPreferences

        @Synchronized
        fun initVoipPref(context: Context) {
            preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        }

        fun updateCallDetails(
            timestamp: Long,
            remoteUserName: String = "",
            remoteUserImage: String? = null,
            callId: Int = -1,
            callType: Int = -1,
            remoteUserAgoraId: Int = -1
        ) {
            val editor = preferenceManager.edit()
            editor.putString(PREF_KEY_CURRENT_REMOTE_USER_NAME, remoteUserName)
            editor.putString(PREF_KEY_CURRENT_REMOTE_USER_IMAGE, remoteUserImage)
            editor.putInt(PREF_KEY_CURRENT_CALL_ID, callId)
            editor.putInt(PREF_KEY_CURRENT_CALL_TYPE, callType)
            editor.putInt(PREF_KEY_CURRENT_REMOTE_USER_AGORA_ID, remoteUserAgoraId)
            editor.putLong(PREF_KEY_CURRENT_CALL_START_TIME, timestamp)
            editor.apply()
        }

        fun updateLastCallDetails() {
            val editor = preferenceManager.edit()
            editor.putString(
                PREF_KEY_LAST_REMOTE_USER_NAME, preferenceManager.getString(
                    PREF_KEY_CURRENT_REMOTE_USER_NAME, ""
                )
            )
            editor.putString(
                PREF_KEY_LAST_REMOTE_USER_IMAGE, preferenceManager.getString(
                    PREF_KEY_CURRENT_REMOTE_USER_IMAGE, null
                )
            )
            editor.putInt(
                PREF_KEY_LAST_CALL_ID, preferenceManager.getInt(
                    PREF_KEY_CURRENT_CALL_ID, -1
                )
            )
            editor.putInt(
                PREF_KEY_LAST_CALL_TYPE, preferenceManager.getInt(
                    PREF_KEY_CURRENT_CALL_TYPE, -1
                )
            )
            editor.putInt(
                PREF_KEY_LAST_REMOTE_USER_AGORA_ID, preferenceManager.getInt(
                    PREF_KEY_CURRENT_REMOTE_USER_AGORA_ID, -1
                )
            )
            editor.putLong(
                PREF_KEY_LAST_CALL_START_TIME, preferenceManager.getLong(
                    PREF_KEY_CURRENT_CALL_START_TIME, 0L
                )
            )

            editor.apply()
        }

        fun updateVoipState(state: Int) {
            Log.d(TAG, "update Webrtc State : $state")
            val editor = preferenceManager.edit()
            editor.putInt(PREF_KEY_WEBRTC_CURRENT_STATE, state)
            editor.apply()
        }

        fun getVoipState(): Int {
            return preferenceManager.getInt(PREF_KEY_WEBRTC_CURRENT_STATE, IDLE)
        }

        fun getStartTimeStamp(): Long {
            return preferenceManager.getLong(PREF_KEY_CURRENT_CALL_START_TIME, 0)
        }

        fun setListener(callTimeStampListener: CallTimeStampListener) {
            preferenceManager.registerOnSharedPreferenceChangeListener(callTimeStampListener)
        }
    }
}

class CallBar {
    val prefListener by lazy { CallTimeStampListener() }

    init {
        VoipPref.setListener(prefListener)
    }

    fun getCallObserver(): ObservableBoolean {
        return prefListener.isCallOnGoing
    }

    fun observerVoipState(): LiveData<Int> {
        return prefListener.observerVoipState()
    }

    fun intentToCallActivity() {
//       TODO: INTENT TO CALL ACTIVITY
    }
}

class CallTimeStampListener : SharedPreferences.OnSharedPreferenceChangeListener {
    val isCallOnGoing by lazy {
        ObservableBoolean(checkTimestamp())
    }

    private val voipStateLiveData by lazy {
        MutableLiveData(checkVoipState())
    }

    fun observerVoipState(): LiveData<Int> {
        return voipStateLiveData
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged: $key")
        when (key) {
            PREF_KEY_CURRENT_CALL_START_TIME -> isCallOnGoing.set(checkTimestamp())
            PREF_KEY_WEBRTC_CURRENT_STATE -> voipStateLiveData.postValue(checkVoipState())
        }
    }

    private fun checkTimestamp(): Boolean {
        val callStartTimestamp = VoipPref.getStartTimeStamp()
        return if (callStartTimestamp != 0L) {
            Log.d(TAG, "onSharedPreferenceChanged: $callStartTimestamp")
            true
        } else {
            Log.d(TAG, "onSharedPreferenceChanged: $callStartTimestamp")
            false
        }
    }

    private fun checkVoipState(): Int {
        val state = VoipPref.getVoipState()
        Log.d(TAG, "checkVoipState: $state")
        return state
    }

}