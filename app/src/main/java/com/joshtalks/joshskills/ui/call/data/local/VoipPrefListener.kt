package com.joshtalks.joshskills.ui.call.data.local

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.model.VoipUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "VoipPrefListener"

object VoipPrefListener : SharedPreferences.OnSharedPreferenceChangeListener {
    private val UI_STATE_UPDATED = setOf(
        PREF_KEY_CURRENT_USER_ON_HOLD,
        PREF_KEY_CURRENT_USER_ON_MUTE,
        PREF_KEY_CURRENT_USER_SPEAKER_ON,
        PREF_KEY_CURRENT_REMOTE_USER_ON_MUTE
    )

    private val timerLiveData by lazy {
        MutableLiveData(checkTimestamp())
    }

    private val voipStateLiveData by lazy {
        MutableLiveData(checkVoipState())
    }

    private val voipUIStateLiveData by lazy {
        MutableStateFlow(checkUIState())
    }

    private val voipUserUIStateLiveData by lazy {
        MutableStateFlow(0)
    }

    fun observerVoipState(): LiveData<Int> {
        return voipStateLiveData
    }

    fun observerVoipUIState(): StateFlow<VoipUIState> {
        return voipUIStateLiveData
    }

    fun observerVoipUserUIState(): StateFlow<Int> {
        return voipUserUIStateLiveData
    }

    fun observerStartTime(): LiveData<Long> {
        return timerLiveData
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged: $key")
        when (key) {
            PREF_KEY_CURRENT_CALL_START_TIME -> timerLiveData.value = checkTimestamp()
            PREF_KEY_CURRENT_REMOTE_USER_NAME -> voipUserUIStateLiveData.value = if(voipUserUIStateLiveData.value == 0) 1 else 0
            PREF_KEY_WEBRTC_CURRENT_STATE -> voipStateLiveData.value = checkVoipState()
            in UI_STATE_UPDATED -> {
                Log.d(TAG, "onSharedPreferenceChanged: $key")
                voipUIStateLiveData.value = checkUIState()
            }
        }
    }

    private fun checkTimestamp(): Long {
        return VoipPref.getStartTimeStamp()
    }

    private fun checkVoipState(): Int {
        val state = VoipPref.getVoipState()
        Log.d(TAG, "checkVoipState: $state")
        return state
    }

    private fun checkUIState(): VoipUIState {
        val state = VoipPref.getVoipUIState()
        Log.d(TAG, "checkUIState: $state")
        return state
    }
}