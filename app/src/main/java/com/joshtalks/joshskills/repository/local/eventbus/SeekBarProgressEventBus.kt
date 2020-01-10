package com.joshtalks.joshskills.repository.local.eventbus


data class SeekBarProgressEventBus(
    var cId: String?,
    var progress: Int
)