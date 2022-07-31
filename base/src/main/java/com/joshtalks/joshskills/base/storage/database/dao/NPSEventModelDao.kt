package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.*
import java.util.*

@Dao
interface NPSEventModelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNPSEvent(npsEventModel: NPSEventModel)

    @Query(value = "SELECT count() FROM nps_event_table where event_name= :eventName AND day= :day AND event_id= :eventId ")
    suspend fun isEventExist(
        eventName: String,
        day: Int,
        eventId: String = EMPTY
    ): Long

    @Query(value = "SELECT COUNT() FROM nps_event_table where created_at >= :startDate AND created_at <= :endDate")
    suspend fun getTotalRecords(startDate: Date, endDate: Date): Long


    @Transaction
    suspend fun getTotalCountOfRows(): Long {
        val startDate = dateStartOfDay()
        val endDate = Date()
        return getTotalRecords(startDate, endDate)
    }
}