package com.joshtalks.joshskills.repository.local.model.assessment

import androidx.room.Embedded
import androidx.room.Relation
import com.joshtalks.joshskills.repository.server.assessment.AssessmentIntro
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse

data class AssessmentWithRelations(

    @Embedded
    val assessment: Assessment,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "assessmentId",
        entity = AssessmentQuestion::class
    )
    val questionList: List<AssessmentQuestionWithRelations>,

    @Relation(
        parentColumn = "remoteId",
        entityColumn = "assessmentId",
        entity = AssessmentIntro::class
    )
    val assessmentIntroList: List<AssessmentIntro>

) {

    constructor(assessmentResponse: AssessmentResponse) : this(
        assessment = Assessment(assessmentResponse),
        questionList = assessmentResponse.questions.map {
            AssessmentQuestionWithRelations(it, assessmentResponse.id)
        },
        assessmentIntroList = assessmentResponse.intro.map {
            AssessmentIntro(it, assessmentResponse.id)
        }
    )

}
