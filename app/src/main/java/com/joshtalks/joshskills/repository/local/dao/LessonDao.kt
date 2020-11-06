package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.repository.local.entity.LessonModel

@Dao
interface LessonDao {
  //  @Query("SELECT * FROM lessonmodel ORDER BY lesson_no DESC")
   // fun getLessons(): DataSource.Factory<Int, LessonModel>

    @Query("SELECT * FROM lessonmodel WHERE lesson_id=:lessonId ORDER BY lesson_no DESC")
    fun getLesson(lessonId: Int): LessonModel?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSingleItem(lesson: LessonModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(lesson: List<LessonModel>)

}