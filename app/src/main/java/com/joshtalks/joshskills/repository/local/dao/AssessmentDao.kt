package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionFeedback
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentIntro
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept

@Dao
abstract class AssessmentDao {

    @Transaction
    @Query("SELECT * FROM assessments WHERE remoteId = :assessmentId")
    abstract fun getAssessmentById(assessmentId: Int): AssessmentWithRelations?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessmentWithRelations: AssessmentWithRelations) {
        insertAssessmentWithoutRelation(assessmentWithRelations.assessment)
        assessmentWithRelations.questionList.forEach { questionWithRelations ->
            insertAssessmentQuestionWithoutRelation(questionWithRelations.question)
            questionWithRelations.choiceList.forEach { choice ->
                insertAssessmentChoice(choice)
            }
            questionWithRelations.reviseConcept?.let { reviseConcept ->
                insertReviseConcept(reviseConcept)
            }
            questionWithRelations.questionFeedback?.let { questionFeedback ->
                insertAssessmentQuestionFeedback(questionFeedback)
            }
        }
        assessmentWithRelations.assessmentIntroList?.forEach { assessmentIntro ->
            insertAssessmentIntro(assessmentIntro)
        }
    }

    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentQuestion(assessmentQuestionWithRelations: AssessmentQuestionWithRelations) {
        insertAssessmentQuestionWithoutRelation(assessmentQuestionWithRelations.question)
        assessmentQuestionWithRelations.choiceList.forEach { choice ->
            insertAssessmentChoice(choice)
            assessmentQuestionWithRelations.reviseConcept?.let { reviseConcept ->
                insertReviseConcept(reviseConcept)
            }
            assessmentQuestionWithRelations.questionFeedback?.let { questionFeedback ->
                insertAssessmentQuestionFeedback(questionFeedback)
            }
        }
    }

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)     //TODO(13/07/20) - Find a better way to deal with this instead of IGNORE
    suspend fun insertAssessmentFromResponse(assessmentResponse: AssessmentResponse) {
        insertAssessmentWithoutRelation(Assessment(assessmentResponse))
        assessmentResponse.questions.forEach { question ->
            insertAssessmentQuestionWithoutRelation(
                AssessmentQuestion(
                    question,
                    assessmentResponse.id
                )
            )
            question.choices.forEach { choice ->
                insertAssessmentChoice(Choice(choice, question.id))
            }
            question.reviseConcept?.let { reviseConcept ->
                insertReviseConcept(ReviseConcept(reviseConcept, question.id))
            }
            question.feedback?.let { questionFeedback ->
                insertAssessmentQuestionFeedback(
                    AssessmentQuestionFeedback(questionFeedback, question.id)
                )
            }
        }
        assessmentResponse.intro?.forEach { assessmentIntro ->
            insertAssessmentIntro(AssessmentIntro(assessmentIntro, assessmentResponse.id))
        }
    }

    @Query(value = "UPDATE assessments SET status = :status where remoteId= :assessmentId ")
    abstract suspend fun updateAssessmentStatus(assessmentId: Int, status: AssessmentStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentWithoutRelation(assessment: Assessment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentQuestionWithoutRelation(assessmentQuestion: AssessmentQuestion)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateAssessmentQuestionWithoutRelation(assessmentQuestion: AssessmentQuestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentChoice(choice: Choice)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateAssessmentChoice(choice: Choice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReviseConcept(reviseConcept: ReviseConcept)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentQuestionFeedback(assessmentQuestionFeedback: AssessmentQuestionFeedback)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateReviseConcept(reviseConcept: ReviseConcept)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessmentIntro(assessmentIntro: AssessmentIntro)

    @Transaction
    @Query("SELECT  localId FROM assessments  WHERE remoteId = :assessmentId LIMIT 1;")
    abstract fun countOfAssessment(assessmentId: Int? = -1): Int

    @Query(value = "UPDATE assessment_choice SET downloadStatus = :status where remoteId= :choiceId ")
    abstract fun updateChoiceDownloadStatusForAudio(choiceId: Int, status: DOWNLOAD_STATUS)

    @Query(value = "UPDATE assessment_choice SET localAudioUrl = :localPath where remoteId= :choiceId ")
    abstract fun updateChoiceLocalPathForAudio(choiceId: Int, localPath: String)

}
