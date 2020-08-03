package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.ui.view_holders.UnlockNextClassViewHolder

data class UnlockNextClassEventBus(
    val conversationId: Int,
    val viewHolder: UnlockNextClassViewHolder
)