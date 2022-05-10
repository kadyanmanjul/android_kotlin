package com.joshtalks.joshskills.voip.pstn

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class PSTNStateReceiver(val scope: CoroutineScope) : PhoneStateListener() {
    private val pstnFlow = MutableSharedFlow<PSTNState>()

    fun observePstnReceiver() : SharedFlow<PSTNState> {
        return pstnFlow
    }

    var state = 0

    override fun onCallStateChanged(newstate: Int, incomingNumber: String) {
        when (newstate) {
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d("DEBUG", "IDLE")
                state = TelephonyManager.CALL_STATE_IDLE
                CoroutineScope(Dispatchers.IO).launch {
                    try{
                        pstnFlow.emit(PSTNState.Idle)
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
                Log.d(TAG, "getCallingState:Idle  $state")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d("DEBUG", "OFFHOOK")
                state = TelephonyManager.CALL_STATE_OFFHOOK
                scope.launch {
                    try{
                        pstnFlow.emit(PSTNState.OnCall)
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
                Log.d(TAG, "getCallingState:OffHook  $state")
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d("DEBUG", "RINGING")
                state = TelephonyManager.CALL_STATE_RINGING
                scope.launch {
                    try{
                        pstnFlow.emit(PSTNState.Ringing)
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
                Log.d(TAG, "getCallingState: Ringing $state")
            }
        }
    }
}

class PSTNServiceReceiver(val scope: CoroutineScope) : BroadcastReceiver() {
    var telephony: TelephonyManager? = null
    val phoneListener = PSTNStateReceiver(scope)

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephony?.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun onDestroy() {
        telephony?.listen(null, PhoneStateListener.LISTEN_NONE)
    }
}
