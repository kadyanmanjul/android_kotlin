package com.joshtalks.joshskills.track

import androidx.room.* // ktlint-disable no-wildcard-imports
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.Instant

@Entity(tableName = "course_usage")
data class CourseUsageModel(
    @PrimaryKey(autoGenerate = true)
    @Expose
    var id: Long = 0,

    @ColumnInfo(name = "start_time")
    var startTime: Long = Instant.now().epochSecond,

    @ColumnInfo(name = "end_time")
    var endTime: Long? = null,

    @ColumnInfo(name = "conversation_id")
    var conversationId: String,

    @ColumnInfo(name = "created")
    var usageDate: Long = Instant.now().epochSecond,

    @ColumnInfo(name = "screen_name")
    var screenName: String? = null
)

@Dao
interface CourseUsageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoCourseUsage(obj: CourseUsageModel)

    @Query("UPDATE course_usage SET end_time =:endTime  WHERE id= (SELECT MAX(id) FROM course_usage) AND end_time IS  NULL")
    fun updateLastCourseUsage(endTime: Long = Instant.now().epochSecond)

    @Query("DELETE FROM course_usage")
    suspend fun deleteAllSyncSession()

    @Query(value = "SELECT * FROM course_usage where start_time IS NOT NULL AND end_time IS NOT NULL")
    suspend fun getAllSession(): List<CourseUsageModel>
}

data class CourseUsageSync(
    @SerializedName("conversation_id")
    var conversationId: String,
    @SerializedName("start_time")
    var startTime: Long,
    @SerializedName("end_time")
    var endTime: Long
)
