package com.joshtalks.joshskills.repository.local.model.assessment

import androidx.room.Embedded
import androidx.room.Relation
import com.joshtalks.joshskills.repository.server.assessment.ChoiceResponse
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept


data class AssessmentQuestionWithRelations(

    @Embedded
    val question: AssessmentQuestion,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "questionId",
        entity = ChoiceResponse::class
    )
    val choiceList: List<ChoiceResponse>,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "questionId",
        entity = ReviseConcept::class
    )
    val reviseConcept: ReviseConcept

)
