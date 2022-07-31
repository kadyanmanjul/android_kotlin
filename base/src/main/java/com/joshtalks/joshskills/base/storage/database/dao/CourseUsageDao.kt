package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.Instant

@Dao
interface CourseUsageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoCourseUsage(obj: CourseUsageModel) : Long

    @Query("UPDATE course_usage SET end_time =:endTime  WHERE id= (SELECT MAX(id) FROM course_usage) AND end_time IS  NULL")
    fun updateLastCourseUsage(endTime: Long = Instant.now().epochSecond)

    @Query("UPDATE course_usage SET end_time =:endTime  WHERE id= :screenId AND end_time IS  NULL")
    fun updateLastCourseUsageViaId(screenId: Long, endTime: Long = Instant.now().epochSecond)

    @Query("DELETE FROM course_usage")
    suspend fun deleteAllSyncSession()

    @Query(value = "SELECT * FROM course_usage where start_time IS NOT NULL AND end_time IS NOT NULL")
    suspend fun getAllSession(): List<CourseUsageModel>
}