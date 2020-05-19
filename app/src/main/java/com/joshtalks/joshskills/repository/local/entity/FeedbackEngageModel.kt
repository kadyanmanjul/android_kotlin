package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.joshtalks.joshskills.core.dateStartOfDay
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "feedback_engage")
data class FeedbackEngageModel(
    @PrimaryKey()
    @Expose
    var id: String,
    @ColumnInfo(name = "created_at")
    @Expose
    var createdAt: Date = Date()

) : Parcelable


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
