package com.joshtalks.joshskills.repository.server.points


import com.google.gson.annotations.SerializedName
import java.util.Date

data class PointsHistoryResponse(
    @SerializedName("points_history_date_list")
    val pointsHistoryDateList: List<PointsHistoryDate>?,
    @SerializedName("total_points")
    val totalPoints: Int?,
    @SerializedName("total_points_text")
    val totalPointsText: String?,
    @SerializedName("is_course_bought")
    val isCourseBought: Boolean = false,
    @SerializedName("expire_date")
    val expiryDate: Date? = null,
)

data class PointsHistoryDate(
    @SerializedName("date")
    val date: String?,
    @SerializedName("points_history_list")
    val pointsHistoryList: List<PointsHistory>?,
    @SerializedName("points_sum")
    val pointsSum: Int?,
    @SerializedName("award_url_list")
    val awardIconList: List<String> = arrayListOf()
)

data class PointsHistory(
    @SerializedName("points")
    val points: Int?,
    @SerializedName("sub_title")
    val subTitle: String?,
    @SerializedName("title")
    val title: Int? = 0
)
