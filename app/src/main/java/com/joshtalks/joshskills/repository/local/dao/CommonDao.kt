package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import timber.log.Timber

@Dao
interface CommonDao {

    @Query("DELETE FROM assessments WHERE remoteId IN (SELECT assessment_id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
    fun deleteAssessmentsFromLessonQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM assessments WHERE remoteId IN (SELECT assessment_id FROM question_table WHERE course_id = :courseId)")
    fun deleteAssessmentsFromQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM SpeakingTopic WHERE id IN (SELECT topic_id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
    fun deleteSpeakingTopicsForCourse(courseId: Int): Int

    @Query("DELETE FROM practise_engagement_table WHERE questionForId IN (SELECT id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
    fun deletePracticesFromLessonQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM practise_engagement_table WHERE questionForId IN (SELECT questionId FROM question_table WHERE course_id = :courseId)")
    fun deletePracticeFromQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId )")
    fun deleteLessonQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM lessonmodel WHERE course = :courseId")
    fun deleteLessonsForCourse(courseId: Int): Int

    @Query("DELETE FROM chat_table WHERE conversation_id IN (SELECT conversation_id FROM course WHERE courseId = :courseId)")
    fun deleteChatsForCourse(courseId: String): Int

    @Query("DELETE FROM course_usage WHERE conversation_id IN (SELECT conversation_id FROM course WHERE courseId = :courseId)")
    fun deleteCourseUsageForCourse(courseId: String): Int

    @Query("DELETE FROM question_table WHERE course_id = :courseId")
    fun deleteQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM video_watch_table WHERE course_id = :courseId")
    fun deleteVideoEngageForCourse(courseId: Int): Int

    @Query("DELETE FROM course WHERE courseId = :courseId")
    fun deleteCourse(courseId: String): Int

    @Transaction
    fun deleteConversationData(courseId: Int) {
        try {
            var numberOfRowsDeleted = -1;
            numberOfRowsDeleted = deleteAssessmentsFromQuestionsForCourse(courseId)
            numberOfRowsDeleted = deleteAssessmentsFromLessonQuestionsForCourse(courseId)
            numberOfRowsDeleted = deleteSpeakingTopicsForCourse(courseId)
            numberOfRowsDeleted = deletePracticeFromQuestionsForCourse(courseId)
            numberOfRowsDeleted = deletePracticesFromLessonQuestionsForCourse(courseId)
            numberOfRowsDeleted = deleteLessonQuestionsForCourse(courseId)
            numberOfRowsDeleted = deleteLessonsForCourse(courseId)
            numberOfRowsDeleted = deleteChatsForCourse(String.format("%d", courseId))
            numberOfRowsDeleted = deleteCourseUsageForCourse(String.format("%d", courseId))
            numberOfRowsDeleted = deleteQuestionsForCourse(courseId)
//            numberOfRowsDeleted = deleteVideoEngageForCourse(courseId)
            numberOfRowsDeleted = deleteCourse(String.format("%d", courseId))
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

}
