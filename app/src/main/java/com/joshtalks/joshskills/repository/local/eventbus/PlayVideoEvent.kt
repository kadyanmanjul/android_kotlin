package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.VideoType

data class PlayVideoEvent (var chatModel: ChatModel)