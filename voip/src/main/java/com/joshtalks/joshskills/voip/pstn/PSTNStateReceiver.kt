package com.joshtalks.joshskills.voip.pstn

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class PSTNStateReceiver : BroadcastReceiver() {

    companion object {
        val pstnFlow = MutableSharedFlow<PSTNState>()
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(applicationContext: Context, intent: Intent) {
        try {
            getCallingState(applicationContext, intent)
        } catch (ex: Exception) {
            print(ex)
        }
    }

    private fun getCallingState(applicationContext: Context, intent: Intent) {

        val telephony =
            applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val customPhoneListener = PhoneStateListener()
        telephony.listen(customPhoneListener, PhoneStateListener.LISTEN_CALL_STATE)
        val stateStr = intent.extras?.getString(TelephonyManager.EXTRA_STATE)
        var state = 0

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_IDLE -> {
                state = TelephonyManager.CALL_STATE_IDLE
                CoroutineScope(Dispatchers.IO).launch {
                    pstnFlow.emit(PSTNState.Idle)
                }
                Log.d(TAG, "getCallingState: Idle $state")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                state = TelephonyManager.CALL_STATE_OFFHOOK
                CoroutineScope(Dispatchers.IO).launch {
                    pstnFlow.emit(PSTNState.OnCall)
                }
                Log.d(TAG, "getCallingState:OffHook  $state")
            }
            TelephonyManager.EXTRA_STATE_RINGING -> {
                state = TelephonyManager.CALL_STATE_RINGING
                CoroutineScope(Dispatchers.IO).launch {
                    pstnFlow.emit(PSTNState.Ringing)
                }
                Log.d(TAG, "getCallingState: Ringing $state")
            }
        }
    }
}