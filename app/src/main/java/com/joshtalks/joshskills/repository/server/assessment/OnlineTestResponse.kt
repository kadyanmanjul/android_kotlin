package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.server.course_detail.VideoModel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OnlineTestResponse(

    @SerializedName("completed")
    val completed: Boolean,

    @SerializedName("message")
    val message: String = EMPTY,

    @SerializedName("question")
    val question: AssessmentQuestionResponse? = null,

    @SerializedName("rule_assessment_question_id")
    val ruleAssessmentQuestionId: String? = null,

    @SerializedName("rule_assessment_id")
    val ruleAssessmentId: Int? = null,

    @SerializedName("video")
    var videoObject : VideoModel,

    @SerializedName("rule_type")
    var questiontype : OnlineTestType,

    @SerializedName("score_text")
    var scoreText : Int?,

    @SerializedName("points_list")
    val pointsList: List<String>?

) : Parcelable

enum class OnlineTestType(val status: String) {

    @SerializedName("TEST")
    TEST("TEST"),

    @SerializedName("TEACH")
    TEACH("TEACH"),
}
