package com.joshtalks.joshskills.ui.course_details

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponseV2
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.DemoCourseDetailsResponse
import com.joshtalks.joshskills.repository.server.onboarding.EnrollMentorWithTestIdRequest
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.util.showAppropriateMsg
import io.branch.referral.util.CurrencyType
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CourseDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val courseDetailsLiveData: MutableLiveData<CourseDetailsResponseV2> = MutableLiveData()
    val demoCourseDetailsLiveData: MutableLiveData<DemoCourseDetailsResponse> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val points100ABtestLiveData = MutableLiveData<ABTestCampaignData?>()

    val repository: ABTestRepository by lazy { ABTestRepository() }
    fun get100PCampaignData(campaign: String, testId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                points100ABtestLiveData.postValue(campaign)
            }
            fetchCourseDetails(testId)
        }
    }

    fun fetchCourseDetails(testId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["test_id"] = testId
                requestParams["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
                requestParams["mentor_id"] = Mentor.getInstance().getId()
                val response = AppObjectController.commonNetworkService.getCourseDetails(requestParams)
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

    fun savePaymentImpression(event: String, eventData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.commonNetworkService.saveNewBuyPageLayoutImpression(
                    mapOf(
                        "event_name" to event,
                        "event_data" to eventData
                    )
                )
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun savePaymentImpressionForCourseExplorePage(event: String, eventData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.commonNetworkService.saveImpressionForExplore(
                    mapOf(
                        "event_name" to event,
                        "event_data" to eventData
                    )
                )
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getEncryptedText(): String {
        return courseDetailsLiveData.value?.paymentData?.encryptedText ?: EMPTY
    }

    fun removeEntryFromPaymentTable(razorpayOrderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.appDatabase.paymentDao().deletePaymentEntry(razorpayOrderId)
        }
    }

    fun getCourseName(): String {
        return try {
            courseDetailsLiveData.value?.cards?.get(0)?.data?.get("name")?.asString ?: EMPTY
        } catch (e: Exception) {
            EMPTY
        }
    }

    fun getTeacherName(): String {
        return try {
            courseDetailsLiveData.value?.cards?.get(0)?.data?.get("teacher_name")?.asString ?: EMPTY
        } catch (e: Exception) {
            EMPTY
        }
    }

    fun getImageUrl(): String {
        return try {
            courseDetailsLiveData.value?.cards?.filter { it.sequenceNumber == 4 }?.get(0)?.data?.get("dp_url")?.asString ?: EMPTY
        } catch (e: Exception) {
            EMPTY
        }
    }

    fun getCoursePrice(): Double {
        return courseDetailsLiveData.value?.paymentData?.discountedAmount?.substring(1)?.toDouble() ?: 0.0
    }
    fun saveBranchPaymentLog(orderInfoId:String,
                             amount: BigDecimal?,
                             testId: Int = 0,
                             courseName: String = EMPTY){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val extras: HashMap<String, Any> = HashMap()
                extras["test_id"] = testId
                extras["orderinfo_id"] = orderInfoId
                extras["currency"] = CurrencyType.INR.name
                extras["amount"] = amount ?: 0.0
                extras["course_name"] = courseName
                extras["device_id"] = Utils.getDeviceId()
                extras["guest_mentor_id"] = Mentor.getInstance().getId()
                extras["payment_done_from"] = "Payment Summary"
                val resp =  AppObjectController.commonNetworkService.savePaymentLog(extras)
            }catch (ex:Exception){
                Log.e("sagar", "setSupportReason: ${ex.message}")
            }
        }
    }
}
