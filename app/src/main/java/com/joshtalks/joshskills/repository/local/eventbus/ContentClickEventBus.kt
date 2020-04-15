package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.minimalentity.CourseContentEntity

data class ContentClickEventBus(var courseContentEntity: CourseContentEntity)