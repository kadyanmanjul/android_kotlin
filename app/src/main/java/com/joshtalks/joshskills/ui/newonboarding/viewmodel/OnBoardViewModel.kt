package com.joshtalks.joshskills.ui.newonboarding.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.IS_GUEST_ENROLLED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTagIdRequest
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTestIdRequest
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class OnBoardViewModel(application: Application) :
    AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val courseListLiveData: MutableLiveData<List<CourseExploreModel>> = MutableLiveData()

    fun getCourseList() = courseListLiveData.value

    fun enrollMentorAgainstTest(testIds: List<Int>) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    val data = EnrollMentorWithTestIdRequest(
                        PrefManager.getStringValue(USER_UNIQUE_ID),
                        Mentor.getInstance().getId(),
                        testIds
                    )
                    val response =
                        AppObjectController.signUpNetworkService.enrollMentorWithTestIds(data)

                    if (response.isSuccessful) {
                        // bottom Sheet Dialog
                        PrefManager.put(IS_GUEST_ENROLLED, value = true)
                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                        return@launch
                    }

                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun enrollMentorAgainstTags(tagIds: List<Int>) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {

                if (Mentor.getInstance().getId().isNotEmpty()) {
                    val data = EnrollMentorWithTagIdRequest(
                        PrefManager.getStringValue(USER_UNIQUE_ID),
                        Mentor.getInstance().getId(),
                        tagIds
                    )
                    val response =
                        AppObjectController.signUpNetworkService.enrollMentorWithTagIds(data)

                    if (response.isSuccessful) {
                        PrefManager.put(IS_GUEST_ENROLLED, true)
                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                        return@launch
                        // bottom Sheet Dialog
                    }

                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun getCourses() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
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

                if (response.isNullOrEmpty().not()) {
                    courseListLiveData.postValue(response)
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }


    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
    }
}
