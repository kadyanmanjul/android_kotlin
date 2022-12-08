package com.joshtalks.joshskills.voip.pstn

import kotlinx.coroutines.flow.SharedFlow

sealed class PSTNState() {
    object Idle : PSTNState()
    object Ringing : PSTNState()
    object OnCall : PSTNState()
}
internal interface PSTNInterface{
    fun registerPstnReceiver()
    fun unregisterPstnReceiver()
    fun observePSTNState(): SharedFlow<PSTNState>
}