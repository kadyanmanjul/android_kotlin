package com.joshtalks.joshskills.common.repository.local.eventbus

data class CourseHeadingSelectedEvent(
    val isSelected: Boolean,
    val id: Int
)
