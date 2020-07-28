package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.assessment.AssessmentType

data class AssessmentButtonClickEvent(
    val assessmentType: AssessmentType,
    val isQuestionAttempted: Boolean,
    val isAnswered: Boolean,
    val assessmentButtonClick: AssessmentButtonClick
)

enum class AssessmentButtonClick {
    SUBMIT, NEXT, REVISE, NONE
}
