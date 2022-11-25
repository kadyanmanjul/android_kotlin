package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.entity.AudioType
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel

data class AudioPlayEventBus(
    var state: Int,
    var chatModel: ChatModel,
    var audioType: AudioType?
)