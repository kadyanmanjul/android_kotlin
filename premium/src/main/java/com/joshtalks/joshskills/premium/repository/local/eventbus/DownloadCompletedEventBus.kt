package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel
import com.joshtalks.joshskills.premium.ui.view_holders.BaseChatViewHolder

data class DownloadCompletedEventBus(var viewHolder: BaseChatViewHolder, var chatModel: ChatModel)