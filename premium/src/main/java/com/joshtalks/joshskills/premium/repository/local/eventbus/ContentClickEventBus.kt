package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.minimalentity.CourseContentEntity

data class ContentClickEventBus(var courseContentEntity: CourseContentEntity)