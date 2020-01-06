package com.joshtalks.joshskills.repository.local.entity

import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import java.io.Serializable

@Entity(tableName = "course")
data class Course(
    @PrimaryKey()
    @SerializedName("id") val courseId: String,

    @ColumnInfo()
    @SerializedName("duration") val duration: Int,

    @ColumnInfo()
    @SerializedName("is_deleted") val is_deleted: Boolean,

    @ColumnInfo(name = "course_name")
    @SerializedName("name") val courseName: String,

    @ColumnInfo(name = "teacher_id")
    @SerializedName("teacher") val teacherId: String,

    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id") val conversationId: String?,

    @ColumnInfo(name = "course_icon")
    @SerializedName("icon") val courseIcon: String?,

    @ColumnInfo(name = "created")
    @SerializedName("created") val created: String?


) : Serializable


@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisterCourses(courseList: List<Course>)


    @Query(value = "select * from course where conversation_id= :conversation_id ")
    suspend fun chooseRegisterCourseMinimal(conversation_id: String): InboxEntity?

/*
    @Query(value = "SELECT * FROM course ")
    suspend fun getRegisterCourseMinimal(): List<InboxEntity>
*/


    @Query(value = "select inbox.* from (SELECT *,co.conversation_id,co.courseId from course co LEFT JOIN chat_table ct ON  co.conversation_id = ct.conversation_id AND is_delete_message=0  LEFT JOIN question_table qt ON ct.chat_id = qt.chatId  ORDER BY created ASC) inbox GROUP BY inbox.conversation_id ORDER BY created DESC")
    suspend fun getRegisterCourseMinimal(): List<InboxEntity>


}