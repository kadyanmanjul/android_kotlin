package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar

import android.content.Context
import android.content.SharedPreferences
import androidx.databinding.ObservableBoolean
import androidx.preference.PreferenceManager

/**
 * Require DataBinding in targeted xml with following instruction ->
 * 1. import @View
 * 2. add variable @callBar of type CallBar class
 * 3. add @CallBarLayout in xml with required attributes
 * 4. use binding adapters setters @startCallTimer (callBar::startTimer) & @onCallBarClick (callBar::intentToCallActivity)
 * 5. toggle visibility using @isCallONGoing ObservableBoolean
 *  Required to set Variable @callBar in Activity as "binding.callBar= CallBar(this)"
 */

class CallBar(val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    var isCallOnGoing=ObservableBoolean(false)

    init {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        preferenceManager?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == CALL_BAR_SHARED_PREF_KEY) {
            val callStatus=sharedPreferences?.getString(CALL_BAR_SHARED_PREF_KEY, CALL_ON_GOING_FALSE)
            if(callStatus== CALL_ON_GOING_TRUE){
                isCallOnGoing.set(true)
            }
            if(callStatus==CALL_ON_GOING_FALSE){
                isCallOnGoing.set(false)
            }
        }
    }

    fun startTimer(){

    }
    fun intentToCallActivity(){
//       TODO: INTENT TO CALL ACTIVITY
    }
}