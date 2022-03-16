package com.joshtalks.joshskills.voip.pstn
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