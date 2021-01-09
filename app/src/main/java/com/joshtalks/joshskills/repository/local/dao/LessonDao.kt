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

    @Query("SELECT * FROM lessonmodel WHERE lesson_id=:lessonId ORDER BY lesson_no DESC")
    fun observeLesson(lessonId: Int): LiveData<LessonModel>

    @Query("UPDATE lessonmodel SET attempt_number = attempt_number+1 WHERE lesson_id= :lessonId")
    fun incrementAttemptNumber(lessonId: Int): Int

    @Query("SELECT attempt_number FROM lessonmodel WHERE lesson_id=:lessonId")
    fun getAttemptNumber(lessonId: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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

}