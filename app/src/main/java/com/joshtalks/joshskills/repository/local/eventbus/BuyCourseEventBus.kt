package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.CourseExploreModel

data class BuyCourseEventBus(
    var courseId: String?,
    val specialOffer: Boolean = false,
    var courseModel: CourseExploreModel? = null
)