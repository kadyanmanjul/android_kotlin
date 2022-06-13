package com.joshtalks.joshskills.core.pstn_states

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.voip.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Require Receiver in Manifest @PSTNStateReceiver with following filters-
 * 1.PHONE_STATE
 * 2.NEW_OUTGOING_CALL
 */

object PstnObserver : PSTNInterface {

    private val applicationContext= AppObjectController.joshApplication
    val scope = CoroutineScope(Dispatchers.IO)
    private val pstnReceiver = PSTNServiceReceiver(scope)
    private var currentPstn : PSTNState = PSTNState.Idle

    init{
        setPstnState()
    }

    private fun setPstnState() {
        registerPstnReceiver()
        scope.launch {
            pstnReceiver.phoneListener.observePstnReceiver().collect {
                Log.d("pstn", "checkPstnState: set $it")
                currentPstn = it
            }
        }
    }

    private fun registerPstnReceiver() {
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PHONE_STATE")
        }
        applicationContext.registerReceiver(pstnReceiver, filter)
    }

    fun unregisterPstnReceiver() {
        applicationContext.unregisterReceiver(pstnReceiver)
    }

    override fun getCurrentPstnState() : PSTNState {
        return currentPstn
    }
}

class PSTNStateReceiver(val scope: CoroutineScope) : PhoneStateListener() {
    private val pstnFlow = MutableSharedFlow<PSTNState>()
    private val TAG = "PstnObserver"

    fun observePstnReceiver() : SharedFlow<PSTNState> {
        return pstnFlow
    }

    var state = 0

    override fun onCallStateChanged(newstate: Int, incomingNumber: String?) {
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