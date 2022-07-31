package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.*
import java.util.*

@Dao
interface FeedbackEngageModelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedbackEngage(feedbackEngageModel: FeedbackEngageModel)

    @Query(value = "SELECT COUNT() FROM feedback_engage where created_at >= :startDate AND created_at <= :endDate")
    suspend fun getTotalRecords(startDate: Date, endDate: Date): Long


    @Transaction
    suspend fun getTotalCountOfRows(): Long {
        val startDate = dateStartOfDay()
        val endDate = Date()
        return getTotalRecords(startDate, endDate)
    }
}