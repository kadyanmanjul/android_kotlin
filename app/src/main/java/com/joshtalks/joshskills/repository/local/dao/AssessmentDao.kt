package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentIntro
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept

@Dao
abstract class AssessmentDao {
    @Transaction
    @Query("SELECT * FROM assessments WHERE remoteId = :assessmentId")
    abstract fun loadAssesment(assessmentId: Int): AssessmentWithRelations

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessmentWithRelations: AssessmentWithRelations) {
        insertAssessmentWithoutRelation(assessmentWithRelations.assessment)
        assessmentWithRelations.questionList.forEach {
            insertAssessmentQuestion(it.question)
            it.choiceList.forEach { choice->
                insertAssessmentChoice(choice)
            }
            it.reviseConcept?.let { reviseConcept->
                insertReviseConcept(reviseConcept)
            }
        }
        assessmentWithRelations.assessmentIntroList.forEach { assessmentIntro ->
            insertAssessmentIntro(assessmentIntro)
        }
    }
    
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentFromResponse(assessmentResponse: AssessmentResponse) {
        insertAssessmentWithoutRelation(Assessment(assessmentResponse))
        assessmentResponse.questions.forEach { question->
            insertAssessmentQuestion(AssessmentQuestion(question,assessmentResponse.id))
            question.choices.forEach { choice->
                insertAssessmentChoice(Choice(choice,question.id))
            }
            question.reviseConcept?.let { reviseConcept->
                insertReviseConcept(ReviseConcept(reviseConcept,question.id))
            }
        }
        assessmentResponse.intro.forEach {  }
        assessmentResponse.intro.forEach { assessmentIntro ->
            insertAssessmentIntro(AssessmentIntro(assessmentIntro,assessmentResponse.id))
        }
    }
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentWithoutRelation(assessment: Assessment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentQuestion(assessmentQuestion: AssessmentQuestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentChoice(choice: Choice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReviseConcept(reviseConcept: ReviseConcept)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentIntro(assessmentIntro: AssessmentIntro)
}
