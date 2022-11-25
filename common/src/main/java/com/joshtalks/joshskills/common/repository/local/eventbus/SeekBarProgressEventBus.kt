package com.joshtalks.joshskills.common.repository.local.eventbus


data class SeekBarProgressEventBus(
    var cId: String?,
    var progress: Int
)