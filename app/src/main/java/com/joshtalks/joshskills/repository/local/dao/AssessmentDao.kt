package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.Choice

@Dao
interface AssessmentDao {
    @Transaction
    @Query("SELECT * FROM assessments WHERE remoteId = :assessmentId")
    fun loadAssesment(assessmentId: Int): AssessmentWithQuestion

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: Assessment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentQuestion(assessmentQuestion: AssessmentQuestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentChoice(choice: Choice)
}
