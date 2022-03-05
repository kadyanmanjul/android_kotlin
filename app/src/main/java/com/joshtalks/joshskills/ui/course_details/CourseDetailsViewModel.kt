package com.joshtalks.joshskills.ui.course_details

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
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.FreeTrialPaymentResponse
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponseV2
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.DemoCourseDetailsResponse
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTestIdRequest
import com.joshtalks.joshskills.repository.server.points.PointsHistoryResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class CourseDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val courseDetailsLiveData: MutableLiveData<CourseDetailsResponseV2> = MutableLiveData()
    val demoCourseDetailsLiveData: MutableLiveData<DemoCourseDetailsResponse> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val pointsHistoryLiveData: MutableLiveData<PointsHistoryResponse> = MutableLiveData()
    var paymentDetailsLiveData = MutableLiveData<FreeTrialPaymentResponse>()


    fun getPaymentDetails(testId: Int, couponCode: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = HashMap<String, Any>()
            data["test_id"] = testId
            data["instance_id"] = PrefManager.getStringValue(INSTANCE_ID, false)
            data["code"] = couponCode
            if (Mentor.getInstance().getId().isNotEmpty()) {
                data["mentor_id"] = Mentor.getInstance().getId()
            }

            try {
                val res =
                    AppObjectController.signUpNetworkService.getFreeTrialPaymentData(data)
                paymentDetailsLiveData.postValue(res)
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
        }
    }


    fun getPointsSummary() {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val response = AppObjectController.commonNetworkService.getUserPointsHistory(
                    Mentor.getInstance().getId()
                )
                if (response.isSuccessful && response.body() != null) {
                    pointsHistoryLiveData.postValue(response.body())
                }
            } catch (ex: Exception) {
                ex.showAppropriateMsg()
            }
        }
    }



    fun fetchCourseDetails(testId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["test_id"] = testId
                requestParams["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
                requestParams["instance_id"] = PrefManager.getStringValue(INSTANCE_ID, false)
                if (Mentor.getInstance().getId().isNotEmpty() && User.getInstance().isVerified) {
                    requestParams["mentor_id"] = Mentor.getInstance().getId()
                }
                val response =
                    AppObjectController.commonNetworkService.getCourseDetails(requestParams)
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    courseDetailsLiveData.postValue(response.body())
                    return@launch
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun fetchDemoCourseDetails() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getDemoCourseDetails()
                if (response.isSuccessful) {
                    response.body()?.let {
                        demoCourseDetailsLiveData.postValue(it)
                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)

                    }
                    return@launch
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun addMoreCourseToFreeTrial(testId: Int) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    val data = EnrollMentorWithTestIdRequest(
                        PrefManager.getStringValue(USER_UNIQUE_ID),
                        Mentor.getInstance().getId(),
                        test_ids = arrayListOf(testId)
                    )
                    val response =
                        AppObjectController.signUpNetworkService.enrollMentorWithTestIds(data)
                    if (response.isSuccessful) {
                        apiCallStatusLiveData.postValue(ApiCallStatus.START)
                        return@launch
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
