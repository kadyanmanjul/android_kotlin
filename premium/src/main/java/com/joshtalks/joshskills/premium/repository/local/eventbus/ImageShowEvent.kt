package com.joshtalks.joshskills.premium.repository.local.eventbus

data class ImageShowEvent(
    var localPath: String?,
    var serverPath: String?,
    var imageId: String? = null
)