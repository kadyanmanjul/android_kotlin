package com.joshtalks.joshskills.repository.server.onboarding

import com.google.gson.annotations.SerializedName

data class CourseEnrolledRequest(
    @SerializedName("mentor_id")
    val mentorId: String,
    @SerializedName("gaid")
    val gaId: String?,
    @SerializedName("heading_ids")
    val headingIds: List<Int>
)