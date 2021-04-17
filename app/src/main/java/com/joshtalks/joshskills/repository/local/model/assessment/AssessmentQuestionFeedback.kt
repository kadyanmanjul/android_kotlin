package com.joshtalks.joshskills.repository.local.model.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.server.assessment.AssessmentQuestionFeedbackResponse
import kotlinx.android.parcel.Parcelize

@Entity(
    tableName = "assessment_question_feedback", foreignKeys = [
        ForeignKey(
            entity = AssessmentQuestion::class,
            parentColumns = arrayOf("remoteId"),
            childColumns = arrayOf("questionId"),
            onDelete = ForeignKey.CASCADE
        )], indices = [
        Index(value = ["questionId"], unique = true),
        Index(value = ["localId"], unique = true)
    ]
)
@Parcelize
data class AssessmentQuestionFeedback(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "localId")
    @SerializedName("localId")
    val localId: Int = 0,

    @ColumnInfo(name = "remoteId")
    @SerializedName("id")
    val remoteId: Int,

    @ColumnInfo
    @SerializedName("questionId")
    val questionId: Int,

    @ColumnInfo
    @SerializedName("correct_answer_heading")
    val correctAnswerHeading: String = EMPTY,

    @ColumnInfo
    @SerializedName("correct_answer_text")
    val correctAnswerText: String = EMPTY,

    @ColumnInfo
    @SerializedName("wrong_answer_heading")
    val wrongAnswerHeading: String = EMPTY,

    @ColumnInfo
    @SerializedName("wrong_answer_text")
    val wrongAnswerText: String = EMPTY,

    @ColumnInfo
    @SerializedName("wrong_answer_heading2")
    val wrongAnswerHeading2: String = EMPTY,

    @ColumnInfo
    @SerializedName("wrong_answer_text2")
    val wrongAnswerText2: String = EMPTY,

    ) : Parcelable {

    constructor(
        assessmentQuestionFeedbackResponse: AssessmentQuestionFeedbackResponse,
        questionId: Int
    ) : this(
        remoteId = assessmentQuestionFeedbackResponse.id,
        questionId = questionId,
        correctAnswerHeading = assessmentQuestionFeedbackResponse.correctAnswerHeading,
        correctAnswerText = assessmentQuestionFeedbackResponse.correctAnswerText,
        wrongAnswerHeading = assessmentQuestionFeedbackResponse.wrongAnswerHeading,
        wrongAnswerText = assessmentQuestionFeedbackResponse.wrongAnswerText,
        wrongAnswerHeading2 = assessmentQuestionFeedbackResponse.wrongAnswerHeading2,
        wrongAnswerText2 = assessmentQuestionFeedbackResponse.wrongAnswerText2
    )

}
