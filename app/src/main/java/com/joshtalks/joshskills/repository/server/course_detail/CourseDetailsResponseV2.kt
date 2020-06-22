package com.joshtalks.joshskills.repository.server.course_detail

import com.google.gson.annotations.SerializedName

data class CourseDetailsResponseV2(
    @SerializedName("cards")
    val cards: List<Card>
)
