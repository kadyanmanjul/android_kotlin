package com.joshtalks.joshskills.premium.ui.payment.order_summary

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.PremiumApplication
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.core.notification.NotificationCategory
import com.joshtalks.joshskills.premium.core.notification.NotificationUtils
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.SUBSCRIPTION_TEST_ID
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.repository.server.CreateOrderResponse
import com.joshtalks.joshskills.premium.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.premium.repository.server.PaymentSummaryResponse
import com.joshtalks.joshskills.premium.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.premium.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.premium.ui.inbox.payment_verify.Payment
import com.joshtalks.joshskills.premium.ui.inbox.payment_verify.PaymentStatus
import com.joshtalks.joshskills.premium.util.showAppropriateMsg
import io.branch.referral.util.CurrencyType
import kotlinx.coroutines.CoroutineScope
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.math.BigDecimal
import org.json.JSONObject

class PaymentSummaryViewModel(application: Application) : AndroidViewModel(application) {
    var context: PremiumApplication = getApplication()

    enum class ViewState {
        PROCESSING, PROCESSED, INTERNET_NOT_AVAILABLE, ERROR_OCCURED
    }

    var phoneNumber = EMPTY
    var responsePaymentSummary = MediatorLiveData<PaymentSummaryResponse>()
    var responseSubscriptionPaymentSummary = MediatorLiveData<PaymentSummaryResponse>()
    var testId: MutableLiveData<String> = MutableLiveData()
    var viewState: MutableLiveData<ViewState>? = null
    val isRegisteredAlready by lazy { Mentor.getInstance().getId().isNotBlank() }
    var isFreeOrderCreated = MutableLiveData<Boolean>(false)
    var isSubscriptionTipUsed = false
    var mTestId = EMPTY

    val abTestRepository: ABTestRepository by lazy { ABTestRepository() }

    val hasRegisteredMobileNumber by lazy {
        (User.getInstance().phoneNumber.isNullOrEmpty().not() || com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
            com.joshtalks.joshskills.premium.core.PAYMENT_MOBILE_NUMBER
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

    fun getEncryptedText(): String {
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
        return if (isSubscriptionTipUsed) {
            responseSubscriptionPaymentSummary.value?.currency ?: "INR"
        } else {
            responsePaymentSummary.value?.currency ?: "INR"
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
                        try {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        }catch (ex:Exception){
                            ex.printStackTrace()
                        }
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
                        try {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        }catch (ex:Exception){
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }

//    fun getOrderDetails(testId: String?, mobileNumber: String) {
//        viewState?.postValue(ViewState.PROCESSING)
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                if (testId.isNullOrEmpty().not() && testId.equals("null").not()) {
//                    mTestId = testId!!
//                }
//                val data = mutableMapOf(
//                    "encrypted_text" to getEncryptedText(),
//                    "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID),
//                    "mobile" to mobileNumber,
//                    "test_id" to getPaymentTestId()
//                )
//                if (isRegisteredAlready&& User.getInstance().isVerified) {
//                    data["mentor_id"] = Mentor.getInstance().getId()
//                }
//                val paymentDetailsResponse: Response<OrderDetailResponse> =
//                    AppObjectController.signUpNetworkService.createPaymentOrder(data).await()
//                logPayNowAnalyticEvents(paymentDetailsResponse.body()?.razorpayOrderId)
//                if (paymentDetailsResponse.code() == 201) {
//                    val response: OrderDetailResponse = paymentDetailsResponse.body()!!
//                    mPaymentDetailsResponse.postValue(response)
//                    MarketingAnalytics.initPurchaseEvent(data, response)
//                    addPaymentEntry(response)
//                } else if (paymentDetailsResponse.code() == 400) {
//                    showToast("Course already exists with this mobile number. Please login with the entered phone number!", Toast.LENGTH_LONG)
//                } else {
//                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
//                }
//                viewState?.postValue(ViewState.PROCESSED)
//            } catch (ex: Exception) {
//                when (ex) {
//                    is HttpException -> {
//                        viewState?.postValue(ViewState.ERROR_OCCURED)
//                    }
//                    is SocketTimeoutException, is UnknownHostException -> {
//                        viewState?.postValue(ViewState.INTERNET_NOT_AVAILABLE)
//                    }
//                    else -> {
//                        viewState?.postValue(ViewState.PROCESSED)
//                        try {
//                            FirebaseCrashlytics.getInstance().recordException(ex)
//                        }catch (ex:Exception){
//                            ex.printStackTrace()
//                        }
//                    }
//                }
//            }
//        }
//    }

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
                    com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                        com.joshtalks.joshskills.premium.core.USER_UNIQUE_ID
                    ),
                    mobileNumber,
                    getEncryptedText(),
                    null
                )
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data.mentorId = Mentor.getInstance().getId()
                }
                val response = AppObjectController.signUpNetworkService.createFreeOrder(data)
                if (response.isSuccessful) {
                    getFreeTrialNotifications(testId)
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
                        try {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        }catch (ex:Exception){
                            ex.printStackTrace()
                        }
                    }
                }
            }
            viewState?.postValue(ViewState.PROCESSED)
        }
    }

    fun updateSubscriptionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.getOnBoardingStatus(
                        Mentor.getInstance().getId(),
                        com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                            com.joshtalks.joshskills.premium.core.USER_UNIQUE_ID
                        )
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        // Update Version Data in local
                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            SUBSCRIPTION_TEST_ID, this.subscriptionTestId)
                        val versionData = VersionResponse.getInstance()
                        versionData.version.let {
                            it.name = this.version.name
                            it.id = this.version.id
                            VersionResponse.update(versionData)
                        }

                        // save Free trial data
                        FreeTrialData.update(this.freeTrialData)

                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.EXPLORE_TYPE, this.exploreType)
                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.IS_SUBSCRIPTION_STARTED,
                            this.subscriptionData.isSubscriptionBought
                        )
                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.REMAINING_SUBSCRIPTION_DAYS,
                            this.subscriptionData.remainingDays
                        )

                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.IS_TRIAL_STARTED, this.freeTrialData.is7DFTBought)
                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.REMAINING_TRIAL_DAYS, this.freeTrialData.remainingDays)
                        com.joshtalks.joshskills.premium.core.PrefManager.put(
                            com.joshtalks.joshskills.premium.core.SHOW_COURSE_DETAIL_TOOLTIP, this.showTooltip5)
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
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

    fun saveTrueCallerImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveTrueCallerImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
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

    fun getFreeTrialNotifications(testId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.utilsAPIService.getFTScheduledNotifications(testId)
                AppObjectController.appDatabase.scheduleNotificationDao().insertAllNotifications(response)
                NotificationUtils(context).removeScheduledNotification(NotificationCategory.APP_OPEN)
                NotificationUtils(context).updateNotificationDb(NotificationCategory.AFTER_LOGIN)
                NotificationUtils(context).updateNotificationDb(NotificationCategory.EVENT_INDEPENDENT)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun logPaymentEvent(data: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.TRACK_JUSPAY_LOG)
                )
                    AppObjectController.commonNetworkService.saveJuspayPaymentLog(
                        mapOf(
                            "mentor_id" to Mentor.getInstance().getId(),
                            "json" to data
                        )
                    )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}