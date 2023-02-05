package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel

class OpenBestPerformerRaceEventBus(
    var chatObj: ChatModel?,
    var videoUrl: String,
    var isSharable: Boolean = false,
    var sharedItem: String? = null
)