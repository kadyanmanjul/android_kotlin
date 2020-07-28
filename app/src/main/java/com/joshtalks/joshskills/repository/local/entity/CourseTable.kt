package com.joshtalks.joshskills.repository.local.entity

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomWarnings
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import io.reactivex.Maybe
import java.io.Serializable
import java.util.*

@Entity(tableName = "course")
data class Course(
    @PrimaryKey()
    @SerializedName("id") val courseId: String,

    @ColumnInfo()
    @SerializedName("duration") val duration: Int,

    @ColumnInfo()
    @SerializedName("is_deleted") val is_deleted: Boolean,

    @ColumnInfo(name = "course_name")
    @SerializedName("name")
    val courseName: String,

    @ColumnInfo(name = "teacher_id")
    @SerializedName("teacher") val teacherId: String,

    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id") val conversationId: String?,

    @ColumnInfo(name = "course_icon")
    @SerializedName("icon") val courseIcon: String?,

    @ColumnInfo(name = "course_created_date")
    @SerializedName("created") val courseCreatedDate: Date?,

    @ColumnInfo(name = "chat_type")
    @SerializedName("chat_type") val chat_type: String?,

    @ColumnInfo(name = "report_status")
    @SerializedName("report_status") val reportStatus: Boolean = false


) : Serializable


@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisterCourses(courseList: List<Course>)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "select * from course where conversation_id= :conversation_id AND is_deleted=0 ")
    suspend fun chooseRegisterCourseMinimal(conversation_id: String): InboxEntity?


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "select * from course where conversation_id= :conversation_id AND is_deleted=0 ")
    fun chooseRegisterCourseMinimalRX(conversation_id: String): Maybe<InboxEntity>


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "select * FROM (SELECT *,co.conversation_id,co.courseId FROM course co  LEFT JOIN chat_table ct ON  co.conversation_id = ct.conversation_id AND is_delete_message=0  LEFT JOIN question_table qt ON ct.chat_id = qt.chatId  ORDER BY created ASC) inbox where is_deleted=0 GROUP BY inbox.conversation_id ORDER BY created,course_created_date DESC")
    suspend fun getRegisterCourseMinimal(): List<InboxEntity>


    @Query(value = "SELECT * from course ORDER BY course_created_date ASC LIMIT 1")
    fun isUserInOfferDays(): Maybe<Course>
}

