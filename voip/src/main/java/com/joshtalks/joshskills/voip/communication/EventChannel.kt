package com.joshtalks.joshskills.voip.communication

import com.joshtalks.joshskills.voip.communication.model.Communication
import com.joshtalks.joshskills.voip.communication.model.OutgoingData
import kotlinx.coroutines.flow.Flow

internal interface EventChannel {
    suspend fun initChannel()
    fun emitEvent(event : OutgoingData)
    fun observeChannelEvents() : Flow<Communication>
}