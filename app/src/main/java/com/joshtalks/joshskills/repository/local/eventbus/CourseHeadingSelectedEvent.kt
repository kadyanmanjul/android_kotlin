package com.joshtalks.joshskills.repository.local.eventbus

data class CourseHeadingSelectedEvent(
    val isSelected: Boolean,
    val id: Int
)
