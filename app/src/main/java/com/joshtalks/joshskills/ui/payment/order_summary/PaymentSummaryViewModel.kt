package com.joshtalks.joshskills.ui.payment.order_summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.CreateOrderResponse
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.repository.server.PaymentSummaryResponse
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
    var responsePaymentSummary = MediatorLiveData<PaymentSummaryResponse>()
    var responseSubscriptionPaymentSummary = MediatorLiveData<PaymentSummaryResponse>()
    var mPaymentDetailsResponse = MediatorLiveData<OrderDetailResponse>()
    var viewState: MutableLiveData<ViewState>? = null
    val isRegisteredAlready by lazy { Mentor.getInstance().getId().isNotBlank() }
    var isFreeOrderCreated = MutableLiveData<Boolean>(false)
    var isSubscriptionTipUsed = false
    var mTestId = EMPTY

    val hasRegisteredMobileNumber by lazy {
        (User.getInstance().phoneNumber.isNotBlank() || PrefManager.getStringValue(
            PAYMENT_MOBILE_NUMBER
        ).isNotBlank())
    }

    init {
        if (viewState == null) {
            viewState = MutableLiveData()
            viewState!!.value = ViewState.PROCESSING
        }
    }

    fun getCourseName(): String {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.courseName ?: EMPTY

        } else {
            return responsePaymentSummary.value?.courseName ?: EMPTY
        }
    }

    fun getTeacherName(): String {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.teacherName ?: EMPTY

        } else {
            return responsePaymentSummary.value?.teacherName ?: EMPTY
        }
    }

    fun getImageUrl(): String {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.imageUrl ?: EMPTY

        } else {
            return responsePaymentSummary.value?.imageUrl ?: EMPTY
        }
    }

    private fun getEncryptedText(): String {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.encryptedText.toString()

        } else {
            return responsePaymentSummary.value?.encryptedText.toString()
        }
    }

    fun getCourseDiscountedAmount(): Double {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.discountedAmount ?: 0.0

        } else {
            return responsePaymentSummary.value?.discountedAmount ?: 0.0
        }
    }

    fun getCourseActualAmount(): Double {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.amount ?: 0.0

        } else {
            return responsePaymentSummary.value?.amount ?: 0.0
        }
    }

    fun haveCoupon(): Boolean {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.couponDetails?.title.isNullOrBlank()

        } else {
            return responsePaymentSummary.value?.couponDetails?.title.isNullOrBlank()
        }
    }

    fun getDiscount(): Int? {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.amount?.minus(responsePaymentSummary.value?.discountedAmount!!)
                ?.toInt()

        } else {
            return responsePaymentSummary.value?.amount?.minus(responsePaymentSummary.value?.discountedAmount!!)
                ?.toInt()
        }
    }


    fun getCurrency(): String {
        if (isSubscriptionTipUsed) {
            return responseSubscriptionPaymentSummary.value?.currency ?: "INR"

        } else {
            return responsePaymentSummary.value?.currency ?: "INR"
        }
    }

    fun setIsSubscriptionTipUsed(isSubscriptionTipUsed: Boolean) {
        this.isSubscriptionTipUsed = isSubscriptionTipUsed
    }

    fun IsSubscriptionTipUsed() =
        this.isSubscriptionTipUsed

    fun getPaymentTestId(): String {
        if (isSubscriptionTipUsed) {
            return responsePaymentSummary.value!!.specialOffer!!.test_id.toString()
        } else {
            return mTestId
        }
    }

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
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
        }
    }

    fun getSubscriptionPaymentDetails(data: HashMap<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                viewState?.postValue(ViewState.PROCESSED)
                val res =
                    AppObjectController.signUpNetworkService.getPaymentSummaryDetails(data)
                responseSubscriptionPaymentSummary.postValue(res)

            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        viewState?.postValue(ViewState.ERROR_OCCURED)
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        viewState?.postValue(ViewState.INTERNET_NOT_AVAILABLE)
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
        }
    }

    fun getOrderDetails(testId: String?, mobileNumber: String) {
        viewState?.postValue(ViewState.PROCESSING)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (testId.isNullOrEmpty().not() && testId.equals("null").not()) {
                    mTestId = testId!!
                }
                val data = mutableMapOf(
                    "encrypted_text" to getEncryptedText(),
                    "instance_id" to PrefManager.getStringValue(INSTANCE_ID, true),
                    "mobile" to mobileNumber,
                    "test_id" to getPaymentTestId()
                )
                if (isRegisteredAlready) {
                    data["mentor_id"] = Mentor.getInstance().getId()
                }
                val paymentDetailsResponse: Response<OrderDetailResponse> =
                    AppObjectController.signUpNetworkService.createPaymentOrder(data).await()
                logPayNowAnalyticEvents(paymentDetailsResponse.body()?.razorpayOrderId)
                if (paymentDetailsResponse.code() == 201) {
                    val response: OrderDetailResponse = paymentDetailsResponse.body()!!
                    mPaymentDetailsResponse.postValue(response)
                    MarketingAnalytics.initPurchaseEvent(data, response)
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
                        FirebaseCrashlytics.getInstance().recordException(ex)
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

    fun createFreeOrder(testId: String, mobileNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                viewState?.postValue(ViewState.PROCESSING)
                val data = CreateOrderResponse(
                    testId,
                    PrefManager.getStringValue(INSTANCE_ID, true),
                    mobileNumber,
                    getEncryptedText(),
                    null
                )
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data.mentorId = Mentor.getInstance().getId()
                }
                val response =
                    AppObjectController.signUpNetworkService.createFreeOrder(data)
                if (response.isSuccessful) {
                    isFreeOrderCreated.postValue(true)
                }
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        viewState?.postValue(ViewState.ERROR_OCCURED)
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        viewState?.postValue(ViewState.INTERNET_NOT_AVAILABLE)
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
            viewState?.postValue(ViewState.PROCESSED)
        }
    }
}
