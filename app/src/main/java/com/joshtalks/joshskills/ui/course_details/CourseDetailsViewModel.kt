package com.joshtalks.joshskills.ui.course_details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.course_detail.Card
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class CourseDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val courseDetailsLiveData: MutableLiveData<List<Card>> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun fetchCourseDetails(testId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["test_id"] = "99"//testId
                requestParams["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    requestParams["mentor_id"] = Mentor.getInstance().getId()
                }
                val response =
                    AppObjectController.commonNetworkService.getCourseDetails(requestParams)
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    courseDetailsLiveData.postValue(response.body()?.cards)
                    return@launch
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
