package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.ChatModel

data class DeleteMessageEventBus(var chatModel: ChatModel)