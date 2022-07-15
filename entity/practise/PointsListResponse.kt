package com.joshtalks.joshskills.base.local.entity.practise


import com.google.gson.annotations.SerializedName

data class PointsListResponse(
    @SerializedName("points_list")
    val pointsList: List<String>?
)