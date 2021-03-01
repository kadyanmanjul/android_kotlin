package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS

data class DownloadMediaEventBus(
    val downloadStatus: DOWNLOAD_STATUS,
    val id: String,
    val type: BASE_MESSAGE_TYPE,
    val url: String? = null,
    var chatModel: ChatModel? = null
)