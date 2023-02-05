package com.joshtalks.joshskills.premium.repository.local.eventbus

data class OpenClickProgressEventBus(
    var id: Int,
    var postion: Int,
    val practiseOpen: Boolean = true
)