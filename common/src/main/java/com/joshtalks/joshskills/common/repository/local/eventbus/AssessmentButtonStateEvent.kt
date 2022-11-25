package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.server.assessment.AssessmentType

data class AssessmentButtonStateEvent(
    val assessmentType: AssessmentType,
    val isQuestionAttempted: Boolean,
    val isAnswered: Boolean
)