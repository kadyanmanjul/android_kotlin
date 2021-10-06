package com.joshtalks.joshskills.ui.course_progress_new

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_EXPIRY_TIME_IN_MS
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewBaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseOverviewViewModel(application: Application) : AndroidViewModel(application) {

    var progressLiveData: MutableLiveData<CourseOverviewBaseResponse> = MutableLiveData()

    var postResponse: MutableLiveData<Boolean> = MutableLiveData()
    fun getCourseOverview(courseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.chatNetworkService.getCourseOverview(
                    Mentor.getInstance().getId(),
                    courseId
                )
                if (response.success) {
                    progressLiveData.postValue(response)
                    response.expiryDate?.let {
                        PrefManager.put(COURSE_EXPIRY_TIME_IN_MS, it.time)
                        PrefManager.put(IS_COURSE_BOUGHT, response.isCourseBought)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
//                postResponse.postValue(true)
            }
        }
    }

    suspend fun getLesson(lessonId: Int): LessonModel? {
        return AppObjectController.appDatabase.lessonDao().getLesson(lessonId)
    }

    suspend fun getLastLessonForCourse(courseId: Int): Int {
        return AppObjectController.appDatabase.lessonDao().getLastLessonForCourse(courseId)
    }
}