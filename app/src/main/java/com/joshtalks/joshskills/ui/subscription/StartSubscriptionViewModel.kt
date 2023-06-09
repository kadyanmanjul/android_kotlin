package com.joshtalks.joshskills.ui.subscription

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StartSubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val subscriptionTestDetailsLiveData: MutableLiveData<CourseExploreModel> = MutableLiveData()

    private suspend fun registerTestImpression(testId: String) {
        try {
            val requestParams: HashMap<String, String> = HashMap()
            requestParams["test_id"] = testId
            requestParams["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
            if (Mentor.getInstance().getId().isNotEmpty()) {
                requestParams["mentor_id"] = Mentor.getInstance().getId()
            }
            val response =
                AppObjectController.commonNetworkService.getCourseDetails(requestParams)
            if (response.isSuccessful) {
                return
            }

        } catch (ignored: Throwable) {
            // Do Nothing
        }
    }

    fun fetchSubscriptionDetails() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
                val resp =
                    AppObjectController.signUpNetworkService.getSubscriptionTestDetails(gaid)
                if (resp.isSuccessful && resp.body() != null) {
                    registerTestImpression(resp.body()!!.id.toString())
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    subscriptionTestDetailsLiveData.postValue(resp.body())
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
