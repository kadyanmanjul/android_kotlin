package com.joshtalks.joshskills.common.core.pstn_states

sealed class PSTNState() {
    object Idle : PSTNState()
    object Ringing : PSTNState()
    object OnCall : PSTNState()
}
internal interface PSTNInterface{
    fun getCurrentPstnState() : PSTNState
}