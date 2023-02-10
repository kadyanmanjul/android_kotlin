package com.joshtalks.joshskills.premium.calling.pstn

import com.joshtalks.joshskills.premium.calling.audiocontroller.AudioRouteConstants
import kotlinx.coroutines.flow.MutableSharedFlow
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