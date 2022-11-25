package com.joshtalks.joshskills.common.repository.server.points


import com.google.gson.annotations.SerializedName

data class PointsInfoResponse(
    @SerializedName("info")
    val info: String?,
    @SerializedName("points_working_list")
    val pointsWorkingList: List<PointsWorking>?
)

data class PointsWorking(
    @SerializedName("label")
    val label: String?,
    @SerializedName("points")
    val points: Int?
)
