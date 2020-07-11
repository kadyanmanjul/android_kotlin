package com.joshtalks.joshskills.repository.local.model.assessment

import androidx.room.Embedded
import androidx.room.Relation
import com.joshtalks.joshskills.repository.server.assessment.AssessmentIntro

data class AssessmentWithQuestion(

    @Embedded
    val assessment: Assessment,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "assessmentId",
        entity = AssessmentQuestion::class
    )
    val questionList: List<QuestionWithChoice>,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "assessmentId",
        entity = AssessmentIntro::class
    )
    val reviseConceptList: List<AssessmentIntro>

)
