package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CommonDao {

    @Query("DELETE FROM assessments WHERE remoteId IN (SELECT assessment_id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
    fun deleteAssessmentsFromLessonQuestionsForCourse(courseId: Int)

    @Query("DELETE FROM assessments WHERE remoteId IN (SELECT assessment_id FROM question_table WHERE course_id = :courseId)")
    fun deleteAssessmentsFromQuestionsForCourse(courseId: Int)

    @Query("DELETE FROM SpeakingTopic WHERE id IN (SELECT topic_id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
    fun deleteSpeakingTopicsForCourse(courseId: Int)

    @Query("DELETE FROM practise_engagement_table WHERE questionForId IN (SELECT id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
    fun deletePracticesFromLessonQuestionsForCourse(courseId: Int)

    @Query("DELETE FROM practise_engagement_table WHERE questionForId IN (SELECT questionId FROM question_table WHERE course_id = :courseId)")
    fun deletePracticeFromQuestionsForCourse(courseId: Int)

    @Query("DELETE FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId )")
    fun deleteLessonQuestionsForCourse(courseId: Int)

    @Query("DELETE FROM lessonmodel WHERE course = :courseId")
    fun deleteLessonsForCourse(courseId: Int)

    @Query("DELETE FROM chat_table WHERE conversation_id IN (SELECT conversation_id FROM course WHERE courseId = :courseId)")
    fun deleteChatsForCourse(courseId: String)

    @Query("DELETE FROM course_usage WHERE conversation_id IN (SELECT conversation_id FROM course WHERE courseId = :courseId)")
    fun deleteCourseUsageForCourse(courseId: String)

    @Query("DELETE FROM question_table WHERE course_id = :courseId")
    fun deleteQuestionsForCourse(courseId: Int)

    @Query("DELETE FROM video_watch_table WHERE course_id = :courseId")
    fun deleteVideoEngageForCourse(courseId: Int)

    @Query("DELETE FROM course WHERE courseId = :courseId")
    fun deleteCourse(courseId: String)

    @Transaction
    fun deleteConversationData(courseId: Int) {
        deleteAssessmentsFromQuestionsForCourse(courseId)
        deleteAssessmentsFromLessonQuestionsForCourse(courseId)
        deleteSpeakingTopicsForCourse(courseId)
        deletePracticeFromQuestionsForCourse(courseId)
        deletePracticesFromLessonQuestionsForCourse(courseId)
        deleteLessonQuestionsForCourse(courseId)
        deleteLessonsForCourse(courseId)
        deleteChatsForCourse(String.format("%d", courseId))
        deleteCourseUsageForCourse(String.format("%d", courseId))
        deleteQuestionsForCourse(courseId)
        deleteVideoEngageForCourse(courseId)
        deleteCourse(String.format("%d", courseId))
    }

}
