package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class GoalSelectionResponse(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("data")
    val data: List<GoalList>
)

data class GoalList(
    @SerializedName("test_id")
    val testId: String? = null,

    @SerializedName("goal")
    val goal: String = "",

    @SerializedName("image_url")
    val imageUrl: String = "",
    var isSelected :Boolean = false
)