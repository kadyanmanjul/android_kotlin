package com.joshtalks.joshskills.ui.cohort_based_course.models

import com.google.gson.annotations.SerializedName

data class CohortModel(
    @SerializedName("slots")
    val slots: ArrayList<CohortItemModel>
)

data class CohortItemModel(
    @SerializedName("name")
    val name: String,
    @SerializedName("time_slot")
    val timeSlot: String
)