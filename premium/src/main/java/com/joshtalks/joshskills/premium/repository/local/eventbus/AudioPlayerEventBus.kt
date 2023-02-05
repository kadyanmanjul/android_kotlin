package com.joshtalks.joshskills.premium.repository.local.eventbus

data class AudioPlayerEventBus(
    var state: Int,
    var id: String,
    val audioUrl:String
)