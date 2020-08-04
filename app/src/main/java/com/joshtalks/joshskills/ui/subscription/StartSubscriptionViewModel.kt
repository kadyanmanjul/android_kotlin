package com.joshtalks.joshskills.ui.subscription

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StartSubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val subscriptionTestDetailsLiveData: MutableLiveData<CourseExploreModel> = MutableLiveData()

    fun fetchSubscriptionDetails() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
                val resp =
                    AppObjectController.signUpNetworkService.getSubscriptionTestDetails(gaid)
                if (resp.isSuccessful && resp.body() != null) {
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
