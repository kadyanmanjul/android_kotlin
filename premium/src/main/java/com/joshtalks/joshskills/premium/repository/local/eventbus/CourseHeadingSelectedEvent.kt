package com.joshtalks.joshskills.premium.repository.local.eventbus

data class CourseHeadingSelectedEvent(
    val isSelected: Boolean,
    val id: Int
)
