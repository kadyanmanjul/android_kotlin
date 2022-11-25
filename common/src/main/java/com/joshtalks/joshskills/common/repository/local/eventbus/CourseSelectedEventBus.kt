package com.joshtalks.joshskills.common.repository.local.eventbus

class CourseSelectedEventBus(
    var flag: Boolean,
    val id: Int?,
    var isAlreadyEnrolled: Boolean = false,
    var whatsappLink: String? = null
)