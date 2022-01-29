package com.joshtalks.joshskills.repository.local.eventbus

data class VideoShowEvent(
    var videoTitle: String?,
    var videoId: String?,
    var videoUrl: String?,
    var videoWidth: Int,
    var videoHeight: Int,
)