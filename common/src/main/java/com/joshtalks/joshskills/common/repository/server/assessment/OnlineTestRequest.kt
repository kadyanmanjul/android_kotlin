package com.joshtalks.joshskills.common.repository.server.assessment


import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.local.model.assessment.AssessmentQuestionWithRelations
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "online_test_request")
data class OnlineTestRequest(

    @SerializedName("question_id")
    @ColumnInfo(name = "question_id")
    val questionId: Int,

    @PrimaryKey
    @SerializedName("lesson_id")
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,

    @SerializedName("status")
    @ColumnInfo(name = "status")
    val status: QuestionStatus,

    @SerializedName("answer")
    @ColumnInfo(name = "answer")
    var answer: String,

    @SerializedName("rule_assessment_question_id")
    @ColumnInfo(name = "rule_assessment_question_id")
    var ruleAssessmentQuestionId: String? = null,

    @SerializedName("answer_order")
    @ColumnInfo(name = "answer_order")
    var answerOrder: List<Int>,

    @SerializedName("time_taken")
    @ColumnInfo(name = "time_taken")
    var timeTaken: Long? = null
) : Parcelable {

    constructor(
        question: AssessmentQuestionWithRelations,
        answer: String,
        answerOrder: List<Int>,
        ruleAssessmentQuestionId: String?,
        lessonId: Int,
        timeTaken: Long?
    ) : this(
        questionId = question.question.remoteId,
        status = question.question.status,
        answer = answer,
        answerOrder = answerOrder,
        ruleAssessmentQuestionId = ruleAssessmentQuestionId,
        lessonId = lessonId,
        timeTaken = timeTaken
    )
}
