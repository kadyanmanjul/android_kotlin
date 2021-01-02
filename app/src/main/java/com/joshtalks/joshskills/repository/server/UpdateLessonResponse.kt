package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import kotlinx.android.parcel.Parcelize

data class UpdateLessonResponse(
    @SerializedName("award_mentor_list")
    val awardMentorList: List<Award>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("response_data")
    val responseData: LESSON_STATUS=LESSON_STATUS.NO,
    @SerializedName("Success")
    val success: Boolean?,
    @SerializedName("outranked")
    val outranked: Boolean?,
    @SerializedName("rank_data")
    val outrankedData: OutrankedDataResponse?,
    @SerializedName("points_list")
    val pointsList: List<String>?,
    )

@Parcelize
data class OutrankedDataResponse(
    @SerializedName("new")
    val new: RankData?,
    @SerializedName("old")
    val old: RankData?
) :Parcelable

@Parcelize
data class RankData(
    @SerializedName("points")
    val points: Int?,
    @SerializedName("rank")
    val rank: Int?
):Parcelable