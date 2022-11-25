package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.entity.ChatModel

data class DeleteMessageEventBus(var chatModel: ChatModel)