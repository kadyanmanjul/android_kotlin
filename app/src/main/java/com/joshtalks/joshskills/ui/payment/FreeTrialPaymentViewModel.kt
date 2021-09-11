package com.joshtalks.joshskills.ui.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.FreeTrialPaymentData
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

class FreeTrialPaymentViewModel(application: Application) : AndroidViewModel(application) {

    var paymentDetailsLiveData = MutableLiveData<FreeTrialPaymentData>()
    var orderDetailsLiveData = MutableLiveData<OrderDetailResponse>()
    var isProcessing = MutableLiveData<Boolean>()

    fun getPaymentDetails(testId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing.postValue(true)
            val data = HashMap<String, Any>()
            data["test_id"] = testId
            data["instance_id"] = PrefManager.getStringValue(INSTANCE_ID, false)

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

    fun getOrderDetails(testId: Int, mobileNumber: String) {
        // viewState?.postValue(PaymentSummaryViewModel.ViewState.PROCESSING)
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing.postValue(true)
            try {
                val data = mutableMapOf(
                    "encrypted_text" to paymentDetailsLiveData.value?.encryptedText,
                    "instance_id" to PrefManager.getStringValue(INSTANCE_ID, false),
                    "mobile" to mobileNumber,
                    "test_id" to testId.toString(),
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

}
