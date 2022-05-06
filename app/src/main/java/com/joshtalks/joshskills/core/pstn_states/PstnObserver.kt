package com.joshtalks.joshskills.core.pstn_states

import android.content.IntentFilter
import android.util.Log
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.voip.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val pstnReceiver = PSTNStateReceiver()
    private var currentPstn : PSTNState = PSTNState.Idle

    init{
        setPstnState()
    }

    private fun setPstnState() {
        registerPstnReceiver()
        CoroutineScope(Dispatchers.IO).launch {
            pstnReceiver.observePstnReceiver().collect {
                Log.d("pstn", "checkPstnState: set $it")
                currentPstn = it
            }
        }
    }

    private fun registerPstnReceiver() {
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PHONE_STATE")
            addAction("android.intent.action.NEW_OUTGOING_CALL")
        }
        applicationContext.registerReceiver(pstnReceiver, filter)
    }
    private fun unregisterPstnReceiver() {
        applicationContext.unregisterReceiver(pstnReceiver)
    }

    override fun getCurrentPstnState() : PSTNState {
        return currentPstn
    }
}