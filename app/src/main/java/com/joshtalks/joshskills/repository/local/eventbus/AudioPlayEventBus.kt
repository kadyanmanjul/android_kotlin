package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.ChatModel

data class AudioPlayEventBus(
    var state: Int,
    var chatModel: ChatModel,
    var audioType: AudioType?
)