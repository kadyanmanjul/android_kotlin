package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.assessment.AssessmentType

data class AssessmentButtonStateEvent(
    val assessmentType: AssessmentType,
    val isQuestionAttempted: Boolean,
    val isAnswered: Boolean
)