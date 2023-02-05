package com.joshtalks.joshskills.premium.repository.local.eventbus

data class MediaProgressEventBus(
    val state: Int,
    val id: String,
    val progress: Float,
    val watchTime: Long = 0L
)
