package com.joshtalks.joshskills.repository.local.model.assessment

import androidx.room.Embedded
import androidx.room.Relation
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept


data class QuestionWithChoice(

    @Embedded
    val question: AssessmentQuestion,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "questionId",
        entity = Choice::class
    )
    val choiceList: List<Choice>,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "questionId",
        entity = ReviseConcept::class
    )
    val reviseConcept: ReviseConcept

)
