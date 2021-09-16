package com.joshtalks.joshskills.repository.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel

@Dao
interface LessonDao {
    //  @Query("SELECT * FROM lessonmodel ORDER BY lesson_no DESC")
    // fun getLessons(): DataSource.Factory<Int, LessonModel>

    @Query("SELECT * FROM lessonmodel WHERE lesson_id=:lessonId ORDER BY lesson_no DESC")
    fun getLesson(lessonId: Int): LessonModel?

    @Query("SELECT status FROM lessonmodel WHERE lesson_no=:lessonNo")
    fun getLessonStatus(lessonNo: Int): LESSON_STATUS?

    @Query("SELECT * FROM lessonmodel WHERE chat_id=:chatId ORDER BY lesson_no DESC")
    fun getLessonFromChatId(chatId: String): LessonModel?

    @Query("SELECT * FROM lessonmodel WHERE lesson_id=:lessonId ORDER BY lesson_no DESC")
    fun observeLesson(lessonId: Int): LiveData<LessonModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingleItem(lesson: LessonModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(lesson: List<LessonModel>)

    @Query("UPDATE lessonmodel SET status = :status WHERE lesson_id= :lessonId")
    suspend fun updateLessonStatus(lessonId: Int, status: LESSON_STATUS)

    @Query("UPDATE lessonmodel SET grammarStatus = :status WHERE lesson_id= :lessonId")
    fun updateGrammarSectionStatus(lessonId: Int, status: LESSON_STATUS)

    @Query("UPDATE lessonmodel SET vocabularyStatus = :status WHERE lesson_id= :lessonId")
    fun updateVocabularySectionStatus(lessonId: Int, status: LESSON_STATUS)

    @Query("UPDATE lessonmodel SET readingStatus = :status WHERE lesson_id= :lessonId")
    fun updateReadingSectionStatus(lessonId: Int, status: LESSON_STATUS)

    @Query("UPDATE lessonmodel SET speakingStatus = :status WHERE lesson_id= :lessonId")
    fun updateSpeakingSectionStatus(lessonId: Int, status: LESSON_STATUS)

    @Query("SELECT MAX(lesson_no) FROM lessonmodel WHERE course =:courseId")
    fun getLastLessonForCourse(courseId: Int): Int

    @Query(value = "SELECT COUNT(lesson_id) FROM lessonmodel where course= :courseId  AND interval>:interval")
    suspend fun nextLessonIntervalExist(courseId: String, interval: Int): Long

    @Query(value = "SELECT COUNT(lesson_id) FROM lessonmodel  WHERE lesson_no IN (:ids)")
    suspend fun getLessonNumbers(ids: ArrayList<Int>): Long

    @Query("SELECT DISTINCT lesson_id FROM lessonmodel WHERE course =:courseId")
    fun getLessonIdsForCourse(courseId: Int): List<Int>

}
