package com.joshtalks.joshskills.track

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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

data class CourseUsageSync(
    @SerializedName("conversation_id")
    var conversationId: String,
    @SerializedName("start_time")
    var startTime: Long,
    @SerializedName("end_time")
    var endTime: Long,
    @SerializedName("course_id")
    var courseId: Int,
    @SerializedName("screen_name")
    var screenName: String

)

data class CourseIdFilerModel(
    @ColumnInfo(name = "conversation_id")
    var conversationId: String,
    @ColumnInfo(name = "courseId")
    var courseId: Int,
)
