package com.joshtalks.joshskills.repository.local.entity.practise


import com.google.gson.annotations.SerializedName

data class PointsListResponse(
    @SerializedName("points_list")
    val pointsList: List<String>?
)