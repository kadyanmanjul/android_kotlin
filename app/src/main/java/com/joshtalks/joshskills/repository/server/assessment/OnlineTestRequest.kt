package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OnlineTestRequest(

    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("status")
    val status: QuestionStatus,

    @SerializedName("answer")
    var answer: String,

    @SerializedName("rule_assessment_question_id")
    var ruleAssessmentQuestionId: String?=null,

    @SerializedName("answer_order")
    var answerOrder: List<Int>


) : Parcelable {

    constructor(
        question : AssessmentQuestionWithRelations,
        answer: String,
        answerOrder: List<Int>,
        ruleAssessmentQuestionId: String?
    ) : this(
        questionId = question.question.remoteId,
        status = question.question.status,
        answer = answer,
        answerOrder = answerOrder,
        ruleAssessmentQuestionId = ruleAssessmentQuestionId
    )
}
