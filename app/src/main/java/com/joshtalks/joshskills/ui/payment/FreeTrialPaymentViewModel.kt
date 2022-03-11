package com.joshtalks.joshskills.ui.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.FreeTrialPaymentResponse
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.repository.server.points.PointsHistoryResponse
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber

class FreeTrialPaymentViewModel(application: Application) : AndroidViewModel(application) {

    var paymentDetailsLiveData = MutableLiveData<FreeTrialPaymentResponse>()
    var orderDetailsLiveData = MutableLiveData<OrderDetailResponse>()
    var isProcessing = MutableLiveData<Boolean>()
    val mentorPaymentStatus: MutableLiveData<Boolean> = MutableLiveData()
    val d2pSyllabusPdfResponse: MutableLiveData<Map<String, String?>> = MutableLiveData()
    val pointsHistoryLiveData: MutableLiveData<PointsHistoryResponse> = MutableLiveData()

    val points100ABtestLiveData = MutableLiveData<ABTestCampaignData?>()
    val syllabusABtestLiveData = MutableLiveData<ABTestCampaignData?>()
    val abtestNewLayoutLiveData = MutableLiveData<ABTestCampaignData?>()

    val repository: ABTestRepository by lazy { ABTestRepository() }
    fun get100PCampaignData(campaign: String) {
         viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                points100ABtestLiveData.postValue(campaign)
            }
        }
    }

    fun getSyllabusCampaignData(campaign: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                syllabusABtestLiveData.postValue(campaign)
            }
        }
    }

    fun getNewLayoutCampaignData(campaign: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                abtestNewLayoutLiveData.postValue(campaign)
            }
        }
    }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.postGoal(goal)
            if (campaign != null) {
                val data = ABTestRepository().getCampaignData(campaign)
                data?.let {
                    val props = JSONObject()
                    props.put("Variant", data?.variantKey ?: EMPTY)
                    props.put("Variable", AppObjectController.gsonMapper.toJson(data?.variableMap))
                    props.put("Campaign", campaign)
                    props.put("Goal", goal)
                    MixPanelTracker().publishEvent(goal, props)
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

    fun getD2pSyllabusPdfData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.signUpNetworkService.getD2pSyllabusPdf()
                if (response.isSuccessful) {
                    d2pSyllabusPdfResponse.postValue(response.body())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getPaymentDetails(testId: Int, couponCode: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing.postValue(true)
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
            isProcessing.postValue(false)
        }
    }

    fun getOrderDetails(testId: String, mobileNumber: String,encryptedText:String) {
        // viewState?.postValue(PaymentSummaryViewModel.ViewState.PROCESSING)
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing.postValue(true)
            try {
                val data = mutableMapOf(
                    "encrypted_text" to encryptedText,
                    "instance_id" to PrefManager.getStringValue(INSTANCE_ID, false),
                    "mobile" to mobileNumber,
                    "test_id" to testId,
                    "mentor_id" to Mentor.getInstance().getId()
                )

                val orderDetailsResponse: Response<OrderDetailResponse> =
                    AppObjectController.signUpNetworkService.createPaymentOrder(data).await()
                if (orderDetailsResponse.code() == 201) {
                    val response: OrderDetailResponse = orderDetailsResponse.body()!!
                    orderDetailsLiveData.postValue(response)
                }
                // viewState?.postValue(PaymentSummaryViewModel.ViewState.PROCESSED)
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        // viewState?.postValue(PaymentSummaryViewModel.ViewState.ERROR_OCCURED)
                        showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        // viewState?.postValue(PaymentSummaryViewModel.ViewState.INTERNET_NOT_AVAILABLE)
                        showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        // viewState?.postValue(PaymentSummaryViewModel.ViewState.PROCESSED)
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
            isProcessing.postValue(false)
        }
    }

    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }
    fun checkMentorIdPaid() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map = mapOf(Pair("mentor_id", Mentor.getInstance().getId()))
                val response = AppObjectController.commonNetworkService.checkMentorPayStatus(map)
                mentorPaymentStatus.postValue(response["payment"] as Boolean)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
