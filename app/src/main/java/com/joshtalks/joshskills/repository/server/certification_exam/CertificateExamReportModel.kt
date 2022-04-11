package com.joshtalks.joshskills.repository.server.certification_exam


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.ui.userprofile.models.Award
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CertificateExamReportModel(
    @SerializedName("heading")
    val heading: String,
    @SerializedName("is_exam_pass")
    val isExamPass: Boolean,
    @SerializedName("max_score")
    val maxScore: Int,
    @SerializedName("score")
    val score: Double,
    @SerializedName("text")
    val text: String,
    @SerializedName("answers")
    val answers: List<UserSelectedAnswer>? = emptyList(),
    @SerializedName("certificate_url")
    var certificateURL: String?,
    @SerializedName("correct")
    val correct: Int,
    @SerializedName("wrong")
    val wrong: Int,
    @SerializedName("unanswered")
    val unanswered: Int,
    @SerializedName("correct_question_percent")
    val percent: Float,
    @SerializedName("total_question")
    val totalQuestion: Int = 0,
    @SerializedName("award_mentor")
    val awardMentor: Award?,
    @SerializedName("points")
    val points: String?,
    @SerializedName("report_id")
    val reportId: Int = 0

) : Parcelable

@Parcelize
data class UserSelectedAnswer(
    @SerializedName("question_id")
    val question: Int,
    @SerializedName("answer_id")
    val answerId: Int,
    @SerializedName("is_answer_correct")
    val isAnswerCorrect: Boolean,
    val isNotAttempt: Boolean? = false
) : Parcelable


enum class QuestionReportType {
    RIGHT, WRONG, UNANSWERED, UNKNOWN
}
