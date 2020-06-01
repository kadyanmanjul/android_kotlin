package com.joshtalks.joshskills.ui.payment.order_summary

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.BranchIOAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.repository.server.PaymentSummaryResponse
import com.joshtalks.joshskills.util.BindableString
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*

class OrderSummaryViewModel(application: Application) : AndroidViewModel(application) {
    var context: JoshApplication = getApplication()

    enum class ViewState {
        PROCESSING, PROCESSED, INTERNET_NOT_AVAILABLE, ERROR_OCCURED
    }

    var text = BindableString()
    var phoneNumber = EMPTY
    private var mTestId: String? = null
    var responsePaymentSummary = MediatorLiveData<PaymentSummaryResponse>()
    var mPaymentDetailsResponse = MediatorLiveData<OrderDetailResponse>()
    var viewState: MutableLiveData<ViewState>? = null

    init {
        if (viewState == null) {
            viewState = MutableLiveData()
            viewState!!.value = ViewState.PROCESSING
        }
    }

    fun getCourseName(): String = responsePaymentSummary.value?.courseName ?: EMPTY

    fun getCourseAmount(): Double = responsePaymentSummary.value?.discountAmount ?: 0.0

    fun getCurrency(): String = responsePaymentSummary.value?.currency ?: "INR"

    @SuppressLint("LogNotTimber")
    fun getPaymentSummaryDetails(data: HashMap<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                viewState?.postValue(ViewState.PROCESSED)
                val res =
                    AppObjectController.signUpNetworkService.getPaymentSummaryDetails(data)
                responsePaymentSummary.postValue(res)

            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        viewState?.postValue(ViewState.ERROR_OCCURED)
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        viewState?.postValue(ViewState.INTERNET_NOT_AVAILABLE)
                    }
                    else -> {
                        Crashlytics.logException(ex)
                    }
                }
            }
        }
    }

    @SuppressLint("LogNotTimber")
    fun getOrderDetails(testId: String?, mobileNumber: String) {
        WorkMangerAdmin.newCourseScreenEventWorker(
            responsePaymentSummary.value?.courseName,
            testId,
            buyInitialize = true
        )
        viewState?.postValue(ViewState.PROCESSING)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (testId.isNullOrEmpty().not() && testId.equals("null").not()) {
                    mTestId = testId!!
                }
                val data = mutableMapOf(
                    "encrypted_text" to responsePaymentSummary.value?.encryptedText,
                    "instance_id" to PrefManager.getStringValue(INSTANCE_ID),
                    "mobile" to mobileNumber,
                    "test_id" to mTestId.toString()
                )
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data["mentor_id"] = Mentor.getInstance().getId()
                }
                Log.d(TAG, "getPaymentDetails() called map ${data}")
                val paymentDetailsResponse: Response<OrderDetailResponse> =
                    AppObjectController.signUpNetworkService.createPaymentOrder(data).await()
                Log.d(TAG, "getPaymentDetails() called map ${paymentDetailsResponse}")

                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.INITIATE_PURCHASE)
                if (paymentDetailsResponse.code() == 201) {
                    Log.d(TAG, "getPaymentDetails() called map ${paymentDetailsResponse.body()}")

                    val response: OrderDetailResponse = paymentDetailsResponse.body()!!
                    mPaymentDetailsResponse.postValue(response)
                }
                viewState?.postValue(ViewState.PROCESSED)
            } catch (ex: Exception) {
                Log.d(TAG, "getPaymentDetails() called map ${ex}")

                when (ex) {
                    is HttpException -> {
                        viewState?.postValue(ViewState.ERROR_OCCURED)
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        viewState?.postValue(ViewState.INTERNET_NOT_AVAILABLE)
                    }
                    else -> {
                        viewState?.postValue(ViewState.PROCESSED)
                        Crashlytics.logException(ex)
                    }
                }
            }
        }
    }
}
