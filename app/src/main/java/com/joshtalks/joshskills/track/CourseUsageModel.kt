package com.joshtalks.joshskills.track

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
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

    @SerializedName("created")
    @ColumnInfo(name = "created")
    var usageDate: Long = Instant.now().epochSecond,

    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversationId")
    var conversationId: String? = null,

    @ColumnInfo(name = "screen_name")
    var screenName: String? = null
)

@Dao
interface CourseUsageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoCourseUsage(obj: CourseUsageModel)

    @Query("UPDATE course_usage SET end_time =:endTime  WHERE id= (SELECT MAX(id) FROM course_usage) AND end_time IS  NULL")
    fun updateLastCourseUsage(endTime: Long = Instant.now().epochSecond)

    @Query(value = "SELECT * FROM course_usage ")
    fun getAllSession(): List<CourseUsageModel>

    @Query("DELETE FROM course_usage")
    suspend fun deleteAllSyncSession()
}
