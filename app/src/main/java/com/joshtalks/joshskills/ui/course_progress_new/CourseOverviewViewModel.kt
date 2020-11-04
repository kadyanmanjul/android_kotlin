package com.joshtalks.joshskills.ui.course_progress_new

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseOverviewViewModel(application: Application) : AndroidViewModel(application) {

    var progressLiveData: MutableLiveData<List<CourseOverviewResponse>> = MutableLiveData()


    fun getCourseOverview(courseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = AppObjectController.chatNetworkService.getCourseOverview(
                courseId,
                Mentor.getInstance().getId()
            )
            if (response.success) {
                progressLiveData.postValue(response.responseData)
            }
        }
    }
}