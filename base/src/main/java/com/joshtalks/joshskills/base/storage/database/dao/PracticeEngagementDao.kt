package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.*

@Dao
interface PracticeEngagementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPractise(practiceEngagementV2: PracticeEngagementV2): Long

    @Query(value = "SELECT * FROM practise_engagement_table where  questionForId= :questionId")
    suspend fun getPractice(questionId: String): List<PracticeEngagementV2>?

    @Query(value = "DELETE FROM practise_engagement_table where   questionForId= :questionId AND uploadStatus=:type")
    suspend fun deleteTempPractise(
        questionId: String,
        type: DOWNLOAD_STATUS = DOWNLOAD_STATUS.UPLOADING
    )

    @Transaction
    suspend fun insertPractiseAfterUploaded(practiceEngagementV2: PracticeEngagementV2) {
        deleteTempPractise(practiceEngagementV2.questionForId!!)
        insertPractise(practiceEngagementV2)
    }
}