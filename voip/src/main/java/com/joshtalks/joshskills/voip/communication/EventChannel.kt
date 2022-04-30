package com.joshtalks.joshskills.voip.communication

import com.joshtalks.joshskills.voip.communication.model.Communication
import com.joshtalks.joshskills.voip.communication.model.OutgoingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

internal interface EventChannel {
    fun emitEvent(event : OutgoingData)
    fun observeChannelEvents() : SharedFlow<Communication>
    fun observeChannelState() : SharedFlow<PubnubState>
    fun reconnect()
    fun onDestroy()
}