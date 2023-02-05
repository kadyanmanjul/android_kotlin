package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel

data class DeleteMessageEventBus(var chatModel: ChatModel)