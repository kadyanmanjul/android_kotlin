package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.freshchat.consumer.sdk.j.ch
import com.joshtalks.joshskills.base.constants.CALL_BAR_SHARED_PREF_KEY
import com.joshtalks.joshskills.base.constants.PREF_KEY_WEBRTC_CURRENT_STATE
import com.joshtalks.joshskills.voip.constant.IDLE
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

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
        lateinit var preferenceManager : SharedPreferences

        @Synchronized
        fun initVoipPref(context: Context) {
            preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        }

        fun updateStartCallTime(timestamp : Long) {
            Log.d(TAG, "update Start Time : $timestamp")
            val editor = preferenceManager.edit()
            editor.putLong(CALL_BAR_SHARED_PREF_KEY, timestamp)
            editor.apply()
        }

        fun updateVoipState(state : Int) {
            Log.d(TAG, "update Webrtc State : $state")
            val editor = preferenceManager.edit()
            editor.putInt(PREF_KEY_WEBRTC_CURRENT_STATE, state)
            editor.apply()
        }

        fun getVoipState() : Int {
            return preferenceManager.getInt(PREF_KEY_WEBRTC_CURRENT_STATE, IDLE)
        }

        fun getStartTimeStamp() : Long {
            return preferenceManager.getLong(CALL_BAR_SHARED_PREF_KEY, 0)
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

    fun getCallObserver() : ObservableBoolean {
        return prefListener.isCallOnGoing
    }

    fun observerVoipState() : LiveData<Int> {
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

    private val voipStateLiveData = MutableLiveData(checkVoipState())

    fun observerVoipState() : LiveData<Int> {
        return voipStateLiveData
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged: $key")
        when(key) {
            CALL_BAR_SHARED_PREF_KEY -> isCallOnGoing.set(checkTimestamp())
            PREF_KEY_WEBRTC_CURRENT_STATE -> voipStateLiveData.postValue(checkVoipState())
        }
    }

    private fun checkTimestamp() : Boolean {
        val callStartTimestamp = VoipPref.getStartTimeStamp()
        return if(callStartTimestamp != 0L) {
            Log.d(TAG, "onSharedPreferenceChanged: $callStartTimestamp")
            true
        } else {
            Log.d(TAG, "onSharedPreferenceChanged: $callStartTimestamp")
            false
        }
    }

    private fun checkVoipState() : Int {
        return VoipPref.getVoipState()
    }

}