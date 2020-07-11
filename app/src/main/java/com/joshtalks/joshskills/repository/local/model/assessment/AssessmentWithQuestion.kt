package com.joshtalks.joshskills.repository.local.model.assessment

import androidx.room.Embedded
import androidx.room.Relation

data class AssessmentWithQuestion(
    @Embedded val assessment: Assessment,
    @Relation(
        parentColumn = "remoteId",
        entityColumn = "assessmentId",
        entity = AssessmentQuestion::class
    )
    val questionList: List<AssessmentQuestion>
)
