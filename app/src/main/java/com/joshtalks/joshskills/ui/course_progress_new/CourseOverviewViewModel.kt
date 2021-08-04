package com.joshtalks.joshskills.ui.course_progress_new

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewBaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseOverviewViewModel(application: Application) : AndroidViewModel(application) {

    var progressLiveData: MutableLiveData<CourseOverviewBaseResponse> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    var postResponse: MutableLiveData<Boolean> = MutableLiveData()
    fun getCourseOverview(courseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
                val response = AppObjectController.chatNetworkService.getCourseOverview(
                    Mentor.getInstance().getId(),
                    courseId
                )
                if (response.success) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    progressLiveData.postValue(response)
                    return@launch
                }
            } catch (e: Exception) {
                e.printStackTrace()
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    suspend fun getLesson(lessonId: Int): LessonModel? {
        return AppObjectController.appDatabase.lessonDao().getLesson(lessonId)
    }

    suspend fun getLastLessonForCourse(courseId: Int): Int {
        return AppObjectController.appDatabase.lessonDao().getLastLessonForCourse(courseId)
    }
}