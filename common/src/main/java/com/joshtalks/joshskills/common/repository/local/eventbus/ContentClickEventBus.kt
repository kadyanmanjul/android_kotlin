package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.minimalentity.CourseContentEntity

data class ContentClickEventBus(var courseContentEntity: CourseContentEntity)