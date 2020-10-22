package com.joshtalks.joshskills.ui.day_wise_course.lesson

import androidx.paging.DataSource
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.dao.LessonDao
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonRepository {
    var lessons: DataSource.Factory<Int, LessonModel>
    var lessonDao: LessonDao = AppObjectController.appDatabase.lessonDao()

    init {
        this.lessons = lessonDao.getLessons()
    }

    fun fetchLessonsForLocal(): DataSource.Factory<Int, LessonModel> {
        return this.lessons
    }

    suspend fun syncLessonsWithServer(courseId: String) = withContext(Dispatchers.IO) {
        launch {
            val lessonListResponse = AppObjectController.chatNetworkService.getLessonList(
                Mentor.getInstance().getId(),
                courseId
            )
            if (lessonListResponse.success) {
                lessonDao.insertAll(lessonListResponse.responseData)
            }
        }
    }

}
