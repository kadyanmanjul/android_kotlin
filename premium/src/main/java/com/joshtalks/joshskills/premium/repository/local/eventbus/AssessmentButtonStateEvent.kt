package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.server.assessment.AssessmentType

data class AssessmentButtonStateEvent(
    val assessmentType: AssessmentType,
    val isQuestionAttempted: Boolean,
    val isAnswered: Boolean
)