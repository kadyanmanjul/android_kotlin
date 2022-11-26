package com.joshtalks.joshskills.common.ui.call.data.local

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.voip.base.constants.*

private const val TAG = "VoipPrefListener"

object VoipPrefListener : SharedPreferences.OnSharedPreferenceChangeListener {
    private val timerLiveData by lazy {
        MutableLiveData(checkTimestamp())
    }

    fun observerStartTime(): LiveData<Long> {
        return timerLiveData
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged: $key")
        when (key) {
            PREF_KEY_CURRENT_CALL_START_TIME -> timerLiveData.value = checkTimestamp()
        }
    }

    private fun checkTimestamp(): Long {
        return VoipPref.getStartTimeStamp()
    }
}