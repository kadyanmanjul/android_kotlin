package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import timber.log.Timber

@Dao
interface CommonDao {

    @Query("DELETE FROM assessments WHERE remoteId IN (SELECT assessment_id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
    fun deleteAssessmentsFromLessonQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM assessments WHERE remoteId IN (SELECT assessment_id FROM question_table WHERE course_id = :courseId)")
    fun deleteAssessmentsFromQuestionsForCourse(courseId: Int): Int

    @Query("DELETE FROM SpeakingTopic WHERE CAST(id AS String) IN (SELECT topic_id FROM lesson_question WHERE lessonId IN (SELECT lesson_id FROM lessonmodel WHERE course = :courseId ))")
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

//    @RawQuery
//    fun deleteCourseDataPartOne(
//        courseId: Int,
//        query: SimpleSQLiteQuery = SimpleSQLiteQuery("DELETE assessments, lessonmodel, lesson_question, SpeakingTopic FROM assessments INNER JOIN lesson_question ON assessments.remoteId = lesson_question.assessment_id INNER JOIN lessonmodel ON lesson_question.lessonId = lessonmodel.lesson_id INNER JOIN SpeakingTopic ON lesson_question.topic_id = SpeakingTopic.id WHERE lessonmodel.course = $courseId")
//    )
//
//    @RawQuery
//    fun deleteCourseDataPartTwo(
//        courseId: Int,
//        query: SimpleSQLiteQuery = SimpleSQLiteQuery("DELETE course, chat_table, course_usage, question_table, video_watch_table FROM course INNER JOIN chat_table ON course.conversation_id = chat_table.conversation_id INNER JOIN course_usage ON chat_table.conversation_id = course_usage.conversation_id INNER JOIN question_table ON course.courseId = question_table.course_id INNER JOIN video_watch_table ON question_table.course_id = video_watch_table.course_id WHERE course.courseId = = $courseId")
//    )

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
