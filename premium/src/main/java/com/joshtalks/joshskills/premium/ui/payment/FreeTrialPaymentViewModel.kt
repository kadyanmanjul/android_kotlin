package com.joshtalks.joshskills.premium.ui.payment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.EMPTY
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.premium.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.premium.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.core.showToast
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.server.FreeTrialPaymentResponse
import com.joshtalks.joshskills.premium.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.premium.ui.inbox.payment_verify.Payment
import com.joshtalks.joshskills.premium.ui.inbox.payment_verify.PaymentStatus
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber

class FreeTrialPaymentViewModel(application: Application) : AndroidViewModel(application) {

    var paymentDetailsLiveData = MutableLiveData<FreeTrialPaymentResponse>()
    var orderDetailsLiveData = MutableLiveData<OrderDetailResponse>()
    var isProcessing = MutableLiveData<Boolean>()
    val mentorPaymentStatus: MutableLiveData<Boolean> = MutableLiveData()
    val abTestRepository: ABTestRepository by lazy { ABTestRepository() }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            abTestRepository.postGoal(goal)
            if (campaign != null) {
                val data = ABTestRepository().getCampaignData(campaign)
                data?.let {
                    MixPanelTracker.publishEvent(MixPanelEvent.GOAL)
                        .addParam(ParamKeys.VARIANT, data?.variantKey ?: EMPTY)
                        .addParam(ParamKeys.VARIABLE, AppObjectController.gsonMapper.toJson(data?.variableMap))
                        .addParam(ParamKeys.CAMPAIGN, campaign)
                        .addParam(ParamKeys.GOAL, goal)
                        .push()
                }
            }
        }
    }

    fun getPaymentDetails(testId: Int, couponCode: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing.postValue(true)
            val data = HashMap<String, Any>()
            data["test_id"] = testId
            data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID, false)
            data["code"] = couponCode
            if (Mentor.getInstance().getId().isNotEmpty()) {
                data["mentor_id"] = Mentor.getInstance().getId()
            }

            try {
                val res =
                    AppObjectController.signUpNetworkService.getFreeTrialPaymentData(data)
                if (res.isSuccessful) {
                    paymentDetailsLiveData.postValue(res.body())
                } else {
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        try {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        }catch (ex:Exception){
                            ex.printStackTrace()
                        }
                    }
                }
            }
            isProcessing.postValue(false)
        }
    }

    fun getOrderDetails(testId: String, mobileNumber: String, encryptedText: String) {
        // viewState?.postValue(PaymentSummaryViewModel.ViewState.PROCESSING)
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing.postValue(true)
            try {
                val data = mutableMapOf(
                    "encrypted_text" to encryptedText,
                    "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID, false),
                    "mobile" to mobileNumber,
                    "test_id" to testId,
                    "mentor_id" to Mentor.getInstance().getId()
                )

                val orderDetailsResponse: Response<OrderDetailResponse> =
                    AppObjectController.signUpNetworkService.createPaymentOrder(data).await()
                Log.e("sagar", "getOrderDetails: ${orderDetailsResponse.code()}")
                if (orderDetailsResponse.code() == 201) {
                    val response: OrderDetailResponse = orderDetailsResponse.body()!!
                    orderDetailsLiveData.postValue(response)
                   // MarketingAnalytics.initPurchaseEvent(data, response)
                    addPaymentEntry(response)
                } else {
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
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
                        try {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        }catch (ex:Exception){
                            ex.printStackTrace()
                        }
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

    fun removeEntryFromPaymentTable(razorpayOrderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.appDatabase.paymentDao().deletePaymentEntry(razorpayOrderId)
        }
    }

     private fun addPaymentEntry(response: OrderDetailResponse) {
        AppObjectController.appDatabase.paymentDao().inertPaymentEntry(
            Payment(
                response.amount,
                response.joshtalksOrderId,
                response.razorpayKeyId,
                response.razorpayOrderId,
                PaymentStatus.CREATED
            )
        )
    }

}
