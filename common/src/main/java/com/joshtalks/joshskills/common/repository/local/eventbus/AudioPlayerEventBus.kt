package com.joshtalks.joshskills.common.repository.local.eventbus

data class AudioPlayerEventBus(
    var state: Int,
    var id: String,
    val audioUrl:String
)