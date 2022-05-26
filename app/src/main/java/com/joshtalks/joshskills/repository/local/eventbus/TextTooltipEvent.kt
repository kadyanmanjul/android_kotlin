package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.ChatModel

data class TextTooltipEvent (var chatModel: ChatModel)