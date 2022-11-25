package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.ui.view_holders.BaseChatViewHolder

data class DownloadCompletedEventBus(var viewHolder: BaseChatViewHolder, var chatModel: ChatModel)