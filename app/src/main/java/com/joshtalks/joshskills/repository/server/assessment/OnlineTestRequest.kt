package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OnlineTestRequest(

    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("lesson_id")
    val lessonId: Int,

    @SerializedName("status")
    val status: QuestionStatus,

    @SerializedName("answer")
    var answer: String,

    @SerializedName("rule_assessment_question_id")
    var ruleAssessmentQuestionId: String?=null,

    @SerializedName("answer_order")
    var answerOrder: List<Int>,

    @SerializedName("gaid")
    var gaid: String

) : Parcelable {

    constructor(
        question : AssessmentQuestionWithRelations,
        answer: String,
        answerOrder: List<Int>,
        ruleAssessmentQuestionId: String?,
        lessonId: Int,
        gaid: String
    ) : this(
        questionId = question.question.remoteId,
        status = question.question.status,
        answer = answer,
        answerOrder = answerOrder,
        ruleAssessmentQuestionId = ruleAssessmentQuestionId,
        lessonId = lessonId,
        gaid = gaid
    )
}
