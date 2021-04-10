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
import java.util.Date

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
    @SerializedName("chat_type") val chatType: String?,

    @ColumnInfo(name = "report_status")
    @SerializedName("report_status") val reportStatus: Boolean = false,

    @ColumnInfo(name = "batch_started")
    @SerializedName("batch_started") val batchStarted: String,

    @ColumnInfo(name = "voicecall_status")
    @SerializedName("voicecall_status") val voiceCallStatus: Boolean = false,

    @ColumnInfo(name = "is_group_active")
    @SerializedName("is_group_active") val isGroupActive: Boolean = false,

    @ColumnInfo(name = "is_points_active")
    @SerializedName("is_points_active") val isPointsActive: Boolean = false,

    ) : Serializable


@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisterCourses(courseList: List<Course>): List<Long>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "select * from course where conversation_id= :conversationId AND is_deleted=0 ")
    fun chooseRegisterCourseMinimal(conversationId: String): InboxEntity?


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "select * from course where conversation_id= :conversationId AND is_deleted=0 ")
    fun chooseRegisterCourseMinimalRX(conversationId: String): Maybe<InboxEntity>


    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "SELECT *, max(created) FROM (SELECT * FROM course co  LEFT JOIN chat_table ct ON  co.conversation_id = ct.conversation_id AND is_delete_message=0  LEFT JOIN question_table qt ON ct.chat_id = qt.chatId   LEFT JOIN lessonmodel lm ON ct.chat_id = lm.chat_id  ORDER BY created ASC) inbox WHERE is_deleted=0 GROUP BY inbox.conversation_id ORDER BY created DESC, course_created_date DESC")
    suspend fun getRegisterCourseMinimal(): List<InboxEntity>


    @Query(value = "SELECT * from course ORDER BY course_created_date ASC LIMIT 1")
    fun isUserInOfferDays(): Maybe<Course>

    @Query(value = "select conversation_id from course")
    fun getAllConversationId(): List<String>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "select * from course where courseId= :courseId AND is_deleted=0  LIMIT 1")
    fun getCourseAccordingId(courseId: String): InboxEntity?


    @Query(value = "select courseId from course where conversation_id= :conversationId AND is_deleted=0  LIMIT 1")
    fun getCourseIdFromConversationId(conversationId: String): String

    @Query(value = "select conversation_id from course where courseId= :courseId AND is_deleted=0  LIMIT 1")
    fun getConversationIdFromCourseId(courseId: String): String

}
