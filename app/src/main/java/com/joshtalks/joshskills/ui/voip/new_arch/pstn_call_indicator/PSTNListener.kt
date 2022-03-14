package com.joshtalks.joshskills.ui.voip.new_arch.pstn_call_indicator
import android.telephony.PhoneStateListener

class PSTNListener:PhoneStateListener() {

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)
    }
    sealed class PSTNState(){
        object Idle: PSTNState()
        object Ringing: PSTNState()
        object OnCall: PSTNState()
    }
}