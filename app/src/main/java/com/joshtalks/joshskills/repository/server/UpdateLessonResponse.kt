package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS

data class UpdateLessonResponse(
    @SerializedName("award_mentor_list")
    val awardMentorList: List<Award>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("response_data")
    val responseData: LESSON_STATUS=LESSON_STATUS.NO,
    @SerializedName("Success")
    val success: Boolean?,
    @SerializedName("points_list")
    val pointsList: List<String>?,
    )