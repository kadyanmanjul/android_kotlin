package com.joshtalks.joshskills.common.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.local.model.assessment.AssessmentWithRelations
import kotlinx.parcelize.Parcelize

@Parcelize
data class AssessmentRequest(

    @SerializedName("id")
    val id: Int,

    @SerializedName("questions")
    val questions: List<AssessmentQuestionRequest>


) : Parcelable {

    constructor(
        assessmentWithRelations: AssessmentWithRelations,
        isCapsuleQuiz: Boolean = false
    ) : this(
        id = assessmentWithRelations.assessment.remoteId,
        questions = assessmentWithRelations.questionList.map {
            AssessmentQuestionRequest(it.question, it.choiceList, isCapsuleQuiz)
        }
    )
}
