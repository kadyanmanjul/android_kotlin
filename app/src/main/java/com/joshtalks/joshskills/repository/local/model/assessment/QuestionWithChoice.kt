package com.joshtalks.joshskills.repository.local.model.assessment

import androidx.room.Embedded
import androidx.room.Relation
import com.joshtalks.joshskills.repository.server.assessment.ChoiceResponse


data class QuestionWithChoice(
    @Embedded val question: AssessmentQuestion,
    @Relation(
        parentColumn = "remoteId",
        entityColumn = "questionId",
        entity = ChoiceResponse::class
    )
    val choiceList: List<ChoiceResponse>
)