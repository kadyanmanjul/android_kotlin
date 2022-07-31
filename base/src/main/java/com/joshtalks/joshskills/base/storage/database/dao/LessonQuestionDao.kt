package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.*
import com.joshtalks.joshskills.base.core.AppObjectController

@Dao
interface LessonQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionForLesson(question: LessonQuestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertQuestionForLessonOnAnyThread(question: LessonQuestion)

    @Update
    suspend fun updateQuestionObject(vararg question: LessonQuestion)

    @Query("UPDATE lesson_question set status = :questionStatus WHERE id=:questionId")
    suspend fun updateQuestionStatus(questionId: String, questionStatus: QUESTION_STATUS)

    @Query("SELECT practice_word,lessonId FROM lesson_question")
    fun getRandomWord(): List<RandomWord>

    @Query("SELECT lessonId FROM lesson_question WHERE id = :questionId")
    fun getLessonIdOfQuestion(questionId: String): Int

    @Query("SELECT * FROM lesson_question WHERE lessonId= :lessonId")
    fun getQuestionsForLesson(lessonId: Int): List<LessonQuestion>

    @Query(value = "SELECT * FROM lesson_question  where id=:questionId")
    suspend fun getLessonQuestionById(questionId: String): LessonQuestion?

    @Transaction
    suspend fun getUpdatedLessonQuestion(questionId: String): LessonQuestion? {
        val question: LessonQuestion? = getLessonQuestionById(questionId)
        if (question != null) {
            when (question.materialType) {
                LessonMaterialType.IM ->
                    question.imageList =
                        AppObjectController.appDatabase.chatDao()
                            .getImagesOfQuestion(questionId = question.id)
                LessonMaterialType.VI ->
                    question.videoList =
                        AppObjectController.appDatabase.chatDao()
                            .getVideosOfQuestion(questionId = question.id)
                LessonMaterialType.AU ->
                    question.audioList =
                        AppObjectController.appDatabase.chatDao()
                            .getAudiosOfQuestion(questionId = question.id)
                LessonMaterialType.PD ->
                    question.pdfList =
                        AppObjectController.appDatabase.chatDao()
                            .getPdfOfQuestion(questionId = question.id)
                else -> {
                    question.imageList =
                        AppObjectController.appDatabase.chatDao()
                            .getImagesOfQuestion(questionId = question.id)
                    question.videoList =
                        AppObjectController.appDatabase.chatDao()
                            .getVideosOfQuestion(questionId = question.id)

                }
            }
            if (question.type == LessonQuestionType.PR) {
                question.practiseEngagementV2 =
                    AppObjectController.appDatabase.practiceEngagementDao()
                        .getPractice(question.id)
                question.imageList = AppObjectController.appDatabase.chatDao()
                    .getImagesOfQuestion(questionId = question.id)
            }
        }
        return question
    }

    @Query("UPDATE lesson_question SET downloadStatus = :status , downloadedLocalPath = :path where id=:lessonQuestionId")
    fun updateDownloadStatus(
        lessonQuestionId: String,
        status: DOWNLOAD_STATUS,
        path: String = EMPTY,
    )

    @Query("SELECT COUNT(id) FROM lesson_question WHERE interval= :interval")
    suspend fun getLessonCount(interval: Int): Long
}