package com.joshtalks.joshskills.voip.pstn

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Require Receiver in Manifest @PSTNStateReceiver with following filters-
 * 1.PHONE_STATE
 * 2.NEW_OUTGOING_CALL
 */

class PSTNListener : PSTNInterface {
    override fun observePSTNState(): MutableSharedFlow<PSTNState> {
       return PSTNStateReceiver.pstnFlow
    }
}