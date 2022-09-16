package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel

import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.FreeTrialPaymentResponse
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.inbox.payment_verify.Payment
import com.joshtalks.joshskills.ui.inbox.payment_verify.PaymentStatus
import com.joshtalks.joshskills.ui.payment.FREE_TRIAL_PAYMENT_TEST_ID
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.CouponListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.FeatureListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.OffersListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.PriceListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.PriceParameterModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.repo.BuyPageRepo
import com.joshtalks.joshskills.ui.special_practice.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class BuyPageViewModel : BaseViewModel() {
    private val buyPageRepo by lazy { BuyPageRepo() }
    val mainDispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    var featureAdapter =  FeatureListAdapter()
    var couponListAdapter =  CouponListAdapter()
    var offersListAdapter =  OffersListAdapter()
    var priceListAdapter =  PriceListAdapter()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()

    var informations = ObservableField(EMPTY)
    var testId: String = FREE_TRIAL_PAYMENT_TEST_ID
    var isGovernmentCourse = ObservableBoolean(false)

    var paymentDetailsLiveData = MutableLiveData<FreeTrialPaymentResponse>()
    var orderDetailsLiveData = MutableLiveData<OrderDetailResponse>()
    var isProcessing = ObservableBoolean(false)
    val mentorPaymentStatus: MutableLiveData<Boolean> = MutableLiveData()

    var itemPosition = 0
    var isDiscount = false

    var isCouponApplied = ObservableBoolean(true)
    var isOfferOrInsertCodeVisible = ObservableBoolean(false)

    val isVideoPopUpShow = ObservableBoolean(false)
    var couponList:List<Coupon>?= null

    fun getCourseContent() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiStatus.postValue(ApiCallStatus.START)
                val response = buyPageRepo.getFeatureList(Integer.parseInt(testId))
                if (response.isSuccessful && response.body() != null) {
                    withContext(mainDispatcher) {
                        featureAdapter.addFeatureList(response.body()?.features)
                        message.what = BUY_COURSE_LAYOUT_DATA
                        message.obj = response.body()!!
                        singleLiveEvent.value = message
                       // informations.set(response.body()?.information)
                    }
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                }

            }catch (e: Exception){
                apiStatus.postValue(ApiCallStatus.FAILED)
                e.printStackTrace()
            }

        }
    }

    //This method is for set coupon and offer list on basis of type coupon or offer
    fun getValidCouponList(methodCallType:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = buyPageRepo.getCouponList()
                apiStatus.postValue(ApiCallStatus.START)
                if (response.isSuccessful && response.body() != null) {
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                    withContext(mainDispatcher) {
                        if (methodCallType == COUPON) {
                            couponListAdapter.addOffersList(response.body()!!.listOfCoupon)
                        }
                        else {
                            response.body()!!.listOfCoupon?.let { offersListAdapter.addOffersList(it) }

                            if (response.body()!!.listOfCoupon?.size ?: 0 >= 1)
                                isOfferOrInsertCodeVisible.set(true)
                            else {
                                isOfferOrInsertCodeVisible.set(false)
                                message.what = APPLY_COUPON_BUTTON_SHOW
                                singleLiveEvent.value = message
                            }
                            couponList = response.body()!!.listOfCoupon
                        }
                        delay(200)
                        message.what = SCROLL_TO_BOTTOM
                        singleLiveEvent.value = message
                    }
                }
            }catch (e: Exception){
                apiStatus.postValue(ApiCallStatus.FAILED)
                e.printStackTrace()
            }
        }
    }

    fun applyCoupon(coupon: Coupon) {
        offersListAdapter.applyCoupon(coupon)
    }

    //this method is get price and if pass coupon code then it will return discount price
    fun getCoursePriceList(code: String?) {
        try {
            viewModelScope.launch(Dispatchers.IO){
                try {
                   // isProcessing.set(true)
                    apiStatus.postValue(ApiCallStatus.START)
                    val response = buyPageRepo.getPriceList(PriceParameterModel(PrefManager.getStringValue(USER_UNIQUE_ID), Integer.parseInt(testId),code))
                    if (response.isSuccessful && response.body() != null) {
                        apiStatus.postValue(ApiCallStatus.SUCCESS)
                        withContext(mainDispatcher) {
                            priceListAdapter.addPriceList(response.body()?.courseDetails)
                        }
                    }
                }catch (e: Exception){
                    apiStatus.postValue(ApiCallStatus.FAILED)

                    e.printStackTrace()
                }

            }
        }catch (ex:Exception){
            apiStatus.postValue(ApiCallStatus.FAILED)
            Log.d("BuyPageViewModel.kt", "SAGAR => getCoursePriceList:130 ")
        }
    }

    val onItemClick: (Coupon, Int, Int, String) -> Unit = { it, type, position, couponType ->
        itemPosition = position
        try {
            when (type) {
                CLICK_ON_OFFER_CARD -> {
                    saveImpressionForBuyPageLayout(COUPON_CODE_APPLIED)
                    if (couponType == APPLY) {
                        isCouponApplied.set(true)
                        try {
                            getCoursePriceList(it.couponCode)
                            isDiscount = true
                        }catch (ex:Exception){
                            Log.d("BuyPageViewModel.kt", "SAGAR => :139 ${ex.message}")
                        }
                    }
                    else {
                        isCouponApplied.set(false)
                        getCoursePriceList(null)
                    }
                }
            }
        }catch (ex:Exception){
            Log.d("BuyPageViewModel.kt", "SAGAR => :145 ${ex.message}")
        }
    }

    fun closeIntroVideoPopUpUi(view: View) {
        message.what = CLOSE_SAMPLE_VIDEO
        singleLiveEvent.value = message
    }

    val onItemPriceClick: (CourseDetailsList, Int, Int, String) -> Unit = { it, type, position, cardType ->
        itemPosition = position
        when (type) {
            CLICK_ON_PRICE_CARD -> {
                message.what = CLICK_ON_PRICE_CARD
                message.obj = it
                message.arg1 = position
                singleLiveEvent.value = message
            }
        }
    }

    val onItemCouponClick: (Coupon, Int, Int, String) -> Unit = { it, type, position, couponType ->
        itemPosition = position
        when (type) {
            CLICK_ON_COUPON_APPLY -> {
                onItemClick(it, CLICK_ON_OFFER_CARD, position, APPLY)
                message.what = CLICK_ON_COUPON_APPLY
                message.obj = it
                message.arg1 = position
                singleLiveEvent.value = message
            }
        }
    }

    fun openCouponListScreen(){
        message.what = OPEN_COUPON_LIST_SCREEN
        singleLiveEvent.value = message
    }

    fun openRatingAndReview(){
        message.what = OPEN_RATING_AND_REVIEW_SCREEN
        singleLiveEvent.value = message
    }

    fun openCourseExplore(){
        message.what = OPEN_COURSE_EXPLORE
        singleLiveEvent.value = message
    }

    fun makePhoneCall(){
        message.what = MAKE_PHONE_CALL
        singleLiveEvent.value = message
    }
    fun getOrderDetails(testId: String, mobileNumber: String, encryptedText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing.set(true)
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
                if (orderDetailsResponse.code() == 201) {
                    val response: OrderDetailResponse = orderDetailsResponse.body()!!
                    //orderDetailsLiveData.postValue(response)
                    withContext(Dispatchers.Main){
                        message.what = ORDER_DETAILS_VALUE
                        message.obj = response
                        singleLiveEvent.value = message
                    }
                    MarketingAnalytics.initPurchaseEvent(data, response)
                    addPaymentEntry(response)
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
            isProcessing.set(false)
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

    fun onBackPress(view: View) {
        message.what = BUY_PAGE_BACK_PRESS
        singleLiveEvent.value = message
    }

    fun applyEnteredCoupon(code : String) {
        saveImpressionForBuyPageLayout(COUPON_CODE_APPLIED)
        Log.e("Sagar", "applyEnteredCoupon: $code")
        if (code.isNotBlank()){
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = buyPageRepo.getCouponFromCode(code)
                    val data = response.body()
                    Log.e("Sagar", "applyEnteredCoupon:reponse: ${data?.couponCode}")
                    if (response.isSuccessful && data != null && data.couponCode != null) {
                        viewModelScope.launch(Dispatchers.Main) {
                            offersListAdapter.applyCoupon(data)
                            message.what = COUPON_APPLIED
                            message.obj = data
                            singleLiveEvent.value = message
                        }
                    } else {
                        showToast("Coupon code is not valid or expired")
                    }
                } catch (e: Exception) {
                    showToast("Coupon code is not valid or expired")
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveImpressionForBuyPageLayout(eventName: String, eventData:Int = 0){
        viewModelScope.launch (Dispatchers.IO){
            buyPageRepo.saveBuyPageImpression(
                mapOf(
                    "event_name" to eventName,
                    "event_data" to eventData
                )
            )
        }
    }
}