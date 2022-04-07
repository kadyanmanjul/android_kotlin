package com.joshtalks.joshskills.voip.pstn

import android.content.IntentFilter
import com.joshtalks.joshskills.voip.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

/**
 * Require Receiver in Manifest @PSTNStateReceiver with following filters-
 * 1.PHONE_STATE
 * 2.NEW_OUTGOING_CALL
 */

class PSTNController(val scope: CoroutineScope) : PSTNInterface {
    private val applicationContext= Utils.context
    private val pstnReceiver = PSTNStateReceiver(scope)

    override fun registerPstnReceiver() {
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PHONE_STATE")
            addAction("android.intent.action.NEW_OUTGOING_CALL")
        }
        applicationContext?.registerReceiver(pstnReceiver, filter)
    }

    override fun unregisterPstnReceiver() {
        applicationContext?.unregisterReceiver(pstnReceiver)
    }

    override fun observePSTNState(): SharedFlow<PSTNState> {
        return pstnReceiver.observePstnReceiver()
    }
}