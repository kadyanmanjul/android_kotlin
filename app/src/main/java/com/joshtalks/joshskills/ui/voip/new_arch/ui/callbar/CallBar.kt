package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.base.constants.CALL_BAR_SHARED_PREF_KEY
import com.joshtalks.joshskills.base.constants.CALL_ON_GOING_FALSE
import com.joshtalks.joshskills.base.constants.CALL_ON_GOING_TRUE
import java.sql.Timestamp

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
class CallBar : SharedPreferences.OnSharedPreferenceChangeListener {
    var isCallOnGoing=ObservableBoolean(false)

    companion object {
        lateinit var preferenceManager : SharedPreferences
        lateinit var callBar: CallBar

        @Synchronized
        fun initVoipPref(context: Context) {
            preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
            callBar = CallBar()
        }

        fun update(timestamp : String) {
            Log.d(TAG, "update: $timestamp")
            val editor = preferenceManager.edit()
            editor.putString(CALL_BAR_SHARED_PREF_KEY, timestamp)
            editor.apply()
        }
    }

    init {
        preferenceManager.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged: $key")
        if (key == CALL_BAR_SHARED_PREF_KEY) {
            val callStatus=sharedPreferences?.getString(CALL_BAR_SHARED_PREF_KEY, "0")
            if(callStatus== CALL_ON_GOING_TRUE){
                Log.d(TAG, "onSharedPreferenceChanged: $callStatus")
                isCallOnGoing.set(true)
            }
            if(callStatus==CALL_ON_GOING_FALSE) {
                Log.d(TAG, "onSharedPreferenceChanged: $callStatus")
                isCallOnGoing.set(false)
            }
        }
    }

    fun intentToCallActivity() {
//       TODO: INTENT TO CALL ACTIVITY
    }
}