package com.joshtalks.joshskills.common.core.pstn_states

import kotlinx.coroutines.flow.SharedFlow

sealed class PSTNState() {
    object Idle : PSTNState()
    object Ringing : PSTNState()
    object OnCall : PSTNState()
}
internal interface PSTNInterface{
    fun getCurrentPstnState() : PSTNState
}