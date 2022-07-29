package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.base.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.base.local.model.assessment.Choice
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AssessmentQuestionRequest(

    @SerializedName("id")
    val id: Int,

    @SerializedName("choices")
    val choices: List<ChoiceRequest>,

    @SerializedName("is_attempted")
    val isAttempted: Boolean = false

) : Parcelable {

    constructor(
        assessmentQuestion: AssessmentQuestion,
        choices: List<Choice>,
        isCapsuleQuiz: Boolean = false
    ) : this(
        id = assessmentQuestion.remoteId,
        choices = if (assessmentQuestion.choiceType == ChoiceType.MATCH_TEXT) {
            choices.filter { it.isSelectedByUser || it.userSelectedOrder != 100 || it.userSelectedOrder != 0 }
                .map {
                    ChoiceRequest(it, choices)
                }
        } else if (isCapsuleQuiz) {
            choices.filter { it.isSelectedByUser }
                .map {
                    ChoiceRequest(it)
                }
        } else {
            choices.filter { it.isSelectedByUser || it.userSelectedOrder != 100 || it.userSelectedOrder != 0 }
                .map {
                    ChoiceRequest(it)
                }
        },
        isAttempted = assessmentQuestion.isAttempted
    )

}
