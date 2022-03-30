package com.joshtalks.joshskills.voip.pstn

import kotlinx.coroutines.flow.MutableSharedFlow

sealed class PSTNState() {
    object Idle : PSTNState()
    object Ringing : PSTNState()
    object OnCall : PSTNState()
}
internal interface PSTNInterface{
    fun observePSTNState():MutableSharedFlow<PSTNState>
}