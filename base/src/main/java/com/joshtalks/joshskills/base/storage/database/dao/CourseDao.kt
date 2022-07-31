package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.*
import io.reactivex.Maybe

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

    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "SELECT *, max(created) FROM (SELECT * FROM course co  LEFT JOIN chat_table ct ON  co.conversation_id = ct.conversation_id AND is_delete_message = 0 LEFT JOIN question_table qt ON ct.chat_id = qt.chatId LEFT JOIN lessonmodel lm ON ct.chat_id = lm.chat_id ORDER BY created ASC) inbox WHERE is_deleted = 0 GROUP BY inbox.conversation_id ORDER BY created DESC, course_created_date DESC")
    suspend fun getRegisterCourseMinimal(): List<InboxEntity>

    @Query(value = "SELECT * from course ORDER BY course_created_date ASC LIMIT 1")
    fun isUserInOfferDays(): Maybe<Course>

    @Query(value = "select conversation_id from course")
    fun getAllConversationId(): List<String>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(value = "select * from course where courseId = :courseId AND is_deleted=0  LIMIT 1")
    suspend fun getCourseFromId(courseId: String): InboxEntity?

    @Query(value = "select courseId from course where conversation_id = :conversationId AND is_deleted = 0 LIMIT 1")
    fun getCourseIdFromConversationId(conversationId: String): String

    @Query(value = "select conversation_id from course where courseId = :courseId AND is_deleted = 0 LIMIT 1")
    suspend fun getConversationIdFromCourseId(courseId: String): String?

    @Query("select courseId, conversation_id from course WHERE conversation_id IN (:ids)")
    suspend fun getCourseIdsFromConversationId(ids: List<String>): List<CourseIdFilerModel>
}