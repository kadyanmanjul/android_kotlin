package com.joshtalks.joshskills.ui.payment.order_summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.BranchIOAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.repository.server.PaymentSummaryResponse
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*

class PaymentSummaryViewModel(application: Application) : AndroidViewModel(application) {
    var context: JoshApplication = getApplication()

    enum class ViewState {
        PROCESSING, PROCESSED, INTERNET_NOT_AVAILABLE, ERROR_OCCURED
    }

    var phoneNumber = EMPTY
    var mTestId: String? = null
    var responsePaymentSummary = MediatorLiveData<PaymentSummaryResponse>()
    var mPaymentDetailsResponse = MediatorLiveData<OrderDetailResponse>()
    var viewState: MutableLiveData<ViewState>? = null
    val isRegisteredAlready by lazy { Mentor.getInstance().getId().isNotBlank() }

    val hasRegisteredMobileNumber by lazy {
        (User.getInstance().phoneNumber.isNotBlank() || PrefManager.getStringValue(
            PAYMENT_MOBILE_NUMBER).isNotBlank())
    }

    init {
        if (viewState == null) {
            viewState = MutableLiveData()
            viewState!!.value = ViewState.PROCESSING
        }
    }

    fun getCourseName(): String = responsePaymentSummary.value?.courseName ?: EMPTY

    fun getCourseDiscountedAmount(): Double = responsePaymentSummary.value?.discountedAmount ?: 0.0

    fun getCourseActualAmount(): Double = responsePaymentSummary.value?.amount ?: 0.0

    fun haveCoupon(): Boolean = responsePaymentSummary.value?.couponDetails?.title.isNullOrBlank()

    fun getDiscount(): Int? =
        responsePaymentSummary.value?.amount?.minus(responsePaymentSummary.value?.discountedAmount!!)
            ?.toInt()

    fun getCurrency(): String = responsePaymentSummary.value?.currency ?: "INR"

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
                if (isRegisteredAlready) {
                    data["mentor_id"] = Mentor.getInstance().getId()
                }
                val paymentDetailsResponse: Response<OrderDetailResponse> =
                    AppObjectController.signUpNetworkService.createPaymentOrder(data).await()
                logPayNowAnalyticEvents(paymentDetailsResponse.body()?.razorpayOrderId)
                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.INITIATE_PURCHASE)
                if (paymentDetailsResponse.code() == 201) {
                    val response: OrderDetailResponse = paymentDetailsResponse.body()!!
                    mPaymentDetailsResponse.postValue(response)
                }
                viewState?.postValue(ViewState.PROCESSED)
            } catch (ex: Exception) {
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
    private fun logPayNowAnalyticEvents(razorpayOrderId: String?) {
        AppAnalytics.create(AnalyticsEvent.PAY_NOW_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, getCourseDiscountedAmount())
            .addParam(AnalyticsEvent.HAVE_COUPON_CODE.NAME, haveCoupon())
            .addParam(AnalyticsEvent.IS_USER_REGISTERD.NAME, isRegisteredAlready)
            .addParam(AnalyticsEvent.RAZOR_PAY_ID.NAME, razorpayOrderId)
            .addParam(AnalyticsEvent.SHOWN_COURSE_PRICE.NAME, getCourseActualAmount()).push()
    }
}
