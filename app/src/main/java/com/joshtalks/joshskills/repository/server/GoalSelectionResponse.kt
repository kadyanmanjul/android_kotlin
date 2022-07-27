package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class GoalSelectionResponse(
    @SerializedName("test_id")
    val testId: String? = null,

    @SerializedName("goal")
    val goal: String = "",
)