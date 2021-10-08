package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.ChatModel

class OpenBestPerformerRaceEventBus(
    var chatObj:ChatModel?,
    var videoUrl: String,
    var isSharable:Boolean=false
)