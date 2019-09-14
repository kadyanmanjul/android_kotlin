package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.view_holders.BaseChatViewHolder

data class DownloadCompletedEventBus(var viewHolder: BaseChatViewHolder, var chatModel: ChatModel)