package com.joshtalks.joshskills.ui.cohort_based_course.models

import com.google.gson.annotations.SerializedName

data class CohortItemModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("time_slot")
    val timeSlot: String
)