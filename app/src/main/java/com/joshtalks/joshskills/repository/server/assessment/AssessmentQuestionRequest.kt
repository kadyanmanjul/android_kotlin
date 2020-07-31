package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
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
        choices: List<Choice>
    ) : this(
        id = assessmentQuestion.remoteId,
        choices = choices.filter { it.isSelectedByUser }.map {
            ChoiceRequest(it)
        },
        isAttempted = assessmentQuestion.isAttempted
    )

}
