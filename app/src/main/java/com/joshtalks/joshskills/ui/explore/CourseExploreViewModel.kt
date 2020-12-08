package com.joshtalks.joshskills.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.course_recommend.BaseResponseCourseList
import com.joshtalks.joshskills.repository.server.course_recommend.Segment
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.LinkedHashSet

class CourseExploreViewModel(application: Application) : AndroidViewModel(application) {
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val languageListLiveData: MutableLiveData<LinkedHashSet<String>> = MutableLiveData()
    val courseListLiveData: MutableLiveData<Map<Int, List<CourseExploreModel>>> = MutableLiveData()
    val recommendSegment: MutableLiveData<List<Segment>> = MutableLiveData()


    fun getCourse(list: ArrayList<InboxEntity>?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = HashMap<String, String>()
                if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
                    data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
                }
                if (PrefManager.getStringValue(INSTANCE_ID, false).isNotEmpty()) {
                    data["instance"] = PrefManager.getStringValue(INSTANCE_ID, false)
                }
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data["mentor"] = Mentor.getInstance().getId()
                }
                if (data.isNullOrEmpty()) {
                    data["is_default"] = "true"
                }

                val response: List<CourseExploreModel> =
                    AppObjectController.signUpNetworkService.exploreCourses(data)

                getAllLanguageFilter(response, list)
                val courseByMap: Map<Int, List<CourseExploreModel>> =
                    response.groupBy { it.languageId }.toSortedMap(compareBy { it })
                courseListLiveData.postValue(courseByMap)
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            }
        }
    }

    private fun getAllLanguageFilter(
        response: List<CourseExploreModel>,
        list: ArrayList<InboxEntity>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val languageSet: LinkedHashSet<String> = linkedSetOf()
            val listIterator =
                response.sortedBy { it.languageId }.toMutableList().listIterator()
            while (listIterator.hasNext()) {
                val courseExploreModel = listIterator.next()
                val resp = list?.find { it.courseId == courseExploreModel.course.toString() }
                if (resp != null) {
                    listIterator.remove()
                }
                languageSet.add(courseExploreModel.language)
            }
            languageListLiveData.postValue(languageSet)
        }
    }

    fun getRecommendCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = mapOf("instance_id" to Mentor.getInstance().getId())
                val response: BaseResponseCourseList =
                    AppObjectController.commonNetworkService.getRecommendCoursesAsync(data)
                response.languageSpecificCourses.sortedBy { it.sortOrder }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}