package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel

import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.OFFER_FOR_YOU_TEXT
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.errorState.BUY_COURSE_FEATURE_ERROR
import com.joshtalks.joshskills.ui.errorState.GET_USER_COUPONS_API_ERROR
import com.joshtalks.joshskills.ui.errorState.COURSE_PRICE_LIST_ERROR
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.FREE_TRIAL_PAYMENT_TEST_ID
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.CouponListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.FeatureListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.OffersListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter.PriceListAdapter
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.repo.BuyPageRepo
import com.joshtalks.joshskills.ui.special_practice.utils.*
import io.branch.referral.util.CurrencyType
import kotlinx.coroutines.*
import org.json.JSONObject
import timber.log.Timber
import java.math.BigDecimal
import java.util.*

class BuyPageViewModel : BaseViewModel() {
    private val buyPageRepo by lazy { BuyPageRepo() }
    val mainDispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    var featureAdapter = FeatureListAdapter()
    var couponListAdapter = CouponListAdapter()
    var offersListAdapter = OffersListAdapter()
    var priceListAdapter = PriceListAdapter()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()

    var testId: String = FREE_TRIAL_PAYMENT_TEST_ID
    var manualCoupon = ObservableField(EMPTY)

    var isDiscount = false

    var couponAppliedCode = ObservableField(EMPTY)
    var isCouponApplied = ObservableBoolean(true)
    var isOfferOrInsertCodeVisible = ObservableBoolean(false)
    var offerForYouText = ObservableField("Offer for you")

    var couponList: List<Coupon>? = null

    var callUsText = ObservableField(EMPTY)

    var isCouponApiCall = ObservableBoolean(true)
    var isPriceApiCall = ObservableBoolean(true)
    var priceText = ObservableField(EMPTY)
    var alreadyReasonSelected: String? = null
    var userPhoneNumber: String? = null
    val abTestRepository by lazy { ABTestRepository() }

    fun isSeeAllButtonShow(): Boolean {
        return PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID
    }

    fun getBuyPageFeature() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = buyPageRepo.getBuyPageFeatureData()
                apiStatus.postValue(ApiCallStatus.SUCCESS)
                withContext(mainDispatcher) {
                    featureAdapter.addFeatureList(response?.features)
                    callUsText.set(response?.callUsText)
                    priceText.set(response?.priceEnglishText)
                    message.what = BUY_COURSE_LAYOUT_DATA
                    message.obj = response
                    singleLiveEvent.value = message
                    delay(5200)
                    message.what = SCROLL_TO_BOTTOM
                    singleLiveEvent.value = message
                }
            } catch (e: Exception) {
                withContext(mainDispatcher) {
                    sendErrorMessage(e.message.toString(), null, BUY_COURSE_FEATURE_ERROR)
                }
                e.printStackTrace()
            }

        }
    }

    //This method is for set coupon and offer list on basis of type coupon or offer
    fun getValidCouponList(methodCallType: String, testId:Int, isCouponApplyOrRemove: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isCouponApiCall.set(true)
                val response = buyPageRepo.getCouponList(testId, getCompletedLessonCount())
                if (response.isSuccessful && response.body() != null) {
                    isCouponApiCall.set(false)
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                    withContext(mainDispatcher) {
                        if (methodCallType == COUPON) {
                            response.body()!!.listOfCoupon?.let { couponListAdapter.addOffersList(it) }
                        } else {
                            response.body()!!.listOfCoupon?.let { offersListAdapter.addOffersList(it) }

                            if ((response.body()!!.listOfCoupon?.size ?: 0) >= 1) {
                                offerForYouText.set(
                                    AppObjectController.getFirebaseRemoteConfig().getString(OFFER_FOR_YOU_TEXT)
                                )
                                isOfferOrInsertCodeVisible.set(true)
                            } else {
                                isOfferOrInsertCodeVisible.set(false)
                                message.what = APPLY_COUPON_BUTTON_SHOW
                                singleLiveEvent.value = message
                            }
                            couponList = response.body()!!.listOfCoupon
                            if (isCouponApplyOrRemove.isEmpty()) {
                                message.what = APPLY_COUPON_FROM_INTENT
                                singleLiveEvent.value = message
                            } else {

                            }
                        }
                    }
                } else {
                    isCouponApiCall.set(true)
                    withContext(mainDispatcher) {
                        sendErrorMessage(response.code().toString(), testId.toString() + " ," + getCompletedLessonCount().toString(), GET_USER_COUPONS_API_ERROR)
                    }
                }
            } catch (e: Exception) {
                isCouponApiCall.set(true)
                withContext(mainDispatcher) {
                    sendErrorMessage(e.message.toString(), testId.toString() + " ," + getCompletedLessonCount().toString(), GET_USER_COUPONS_API_ERROR)
                }
                e.printStackTrace()
            }
        }
    }

    fun applyCoupon(coupon: Coupon) {
        offersListAdapter.applyCoupon(coupon)
    }

    //this method is get price and if pass coupon code then it will return discount price
    fun getCoursePriceList(code: String?, validDuration:Date?, couponType:String?) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    isPriceApiCall.set(true)
                    val response = buyPageRepo.getPriceList(code)
                    if (response.isSuccessful && response.body() != null) {
                        apiStatus.postValue(ApiCallStatus.SUCCESS)
                        isPriceApiCall.set(false)
                        withContext(mainDispatcher) {
                            priceListAdapter.addPriceList(response.body()?.courseDetails, validDuration,couponType)
                        }
                    } else {
                        isPriceApiCall.set(true)
                        withContext(mainDispatcher) {
                            sendErrorMessage(exception = code.toString(), payload = code.toString(), apiErrorCode = COURSE_PRICE_LIST_ERROR)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("sagar", "getCoursePriceList: $e")
                    withContext(mainDispatcher) {
                        sendErrorMessage(exception = e.message.toString(), payload = code.toString(), apiErrorCode = COURSE_PRICE_LIST_ERROR)
                    }
                    isPriceApiCall.set(true)
                    e.printStackTrace()
                }

            }
        } catch (ex: Exception) {
            Log.d("BuyPageViewModel.kt", "SAGAR => getCoursePriceList:130 ")
        }
    }

    private fun sendErrorMessage(exception: String?, payload: String?, apiErrorCode: Int) {
        val map = HashMap<String, String?>()
        map["exception"] = exception
        map["payload"] = payload
        message.what = apiErrorCode
        message.obj = map
        singleLiveEvent.value = message
    }

    val onItemClick: (Coupon, Int, Int, String) -> Unit = { it, type, position, couponType ->
        try {
            when (type) {
                CLICK_ON_OFFER_CARD -> {
                    if (couponType == APPLY) {
                        if (couponAppliedCode.get() != it.couponCode){
                            saveImpressionForBuyPageLayout(COUPON_CODE_APPLIED, it.couponCode)
                            isCouponApplied.set(true)
                            if (it.couponCode != manualCoupon.get())
                                couponAppliedCode.set(it.couponCode)
                            else
                                couponAppliedCode.set(manualCoupon.get())
                            try {
                                getCoursePriceList(it.couponCode, it.validDuration, it.couponType)
                                isDiscount = true
                            } catch (ex: Exception) {
                                Log.d("BuyPageViewModel.kt", "SAGAR => :139 ${ex.message}")
                            }
                            if (position!=1) {
                                message.what = APPLY_COUPON_FROM_BUY_PAGE
                                message.obj = it
                                singleLiveEvent.value = message
                            }
                        }else{
                            if (it.isMentorSpecificCoupon!=null && couponAppliedCode.get().isNullOrEmpty())
                                showToast("Coupon already applied")
                        }
                    } else {
                        if (it.isMentorSpecificCoupon == null){
                            getValidCouponList(OFFERS, Integer.parseInt(testId), isCouponApplyOrRemove = REMOVE)
                        }
                        couponAppliedCode.set(EMPTY)
                        saveImpressionForBuyPageLayout(COUPON_CODE_REMOVED, it.couponCode)
                        isCouponApplied.set(false)
                        getCoursePriceList(null,null,null)
                    }
                }
            }
        } catch (ex: Exception) {
            Log.d("BuyPageViewModel.kt", "SAGAR => :145 ${ex.message}")
        }
    }

    val onItemPriceClick: (CourseDetailsList, Int, Int, String) -> Unit =
        { it, type, position, cardType ->
            when (type) {
                CLICK_ON_PRICE_CARD -> {
                    message.what = CLICK_ON_PRICE_CARD
                    message.obj = it
                    message.arg1 = position
                    singleLiveEvent.value = message
                }
            }
        }

    suspend fun getCompletedLessonCount(courseId: String = PrefManager.getStringValue(CURRENT_COURSE_ID)) =
        AppObjectController.appDatabase.lessonDao().getCompletedLessonCount(courseId.toInt())

    //I am making position means come from Insert coupon flow
    // position = 1 come from insert flow
    val onItemCouponClick: (Coupon, Int, Int, String) -> Unit = { it, type, position, couponType ->
        when (type) {
            CLICK_ON_COUPON_APPLY -> {
                if (couponAppliedCode.get() != it.couponCode) {
                    if (couponType == APPLY) {
                        onItemClick(it, CLICK_ON_OFFER_CARD, 1, couponType)
                        message.what = CLICK_ON_COUPON_APPLY
                        message.obj = it
                        message.arg1 = position
                        singleLiveEvent.value = message
                    }
                }else{
                    showToast("Coupon already applied")
                }
            }
        }
    }

    fun openCouponListScreen() {
        message.what = OPEN_COUPON_LIST_SCREEN
        singleLiveEvent.value = message
    }

    fun openRatingAndReview() {
        message.what = OPEN_RATING_AND_REVIEW_SCREEN
        singleLiveEvent.value = message
    }

    fun openCourseExplore() {
        message.what = OPEN_COURSE_EXPLORE
        singleLiveEvent.value = message
    }

    fun makePhoneCall() {
        message.what = MAKE_PHONE_CALL
        singleLiveEvent.value = message
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
                LogException.catchException(ex)
                Timber.e(ex)
            }
        }
    }

    fun removeEntryFromPaymentTable(razorpayOrderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.appDatabase.paymentDao().deletePaymentEntry(razorpayOrderId)
        }
    }

    fun onBackPress(view: View) {
        message.what = BUY_PAGE_BACK_PRESS
        singleLiveEvent.value = message
    }

    //isApplyFrom = 1 => Coupon Fragment
    //isApplyFrom = 0 => Null Apply from Anywhere
    fun applyEnteredCoupon(code: String, isFromLink: Int, isApplyFrom:Int = 0) {
        saveImpressionForBuyPageLayout(COUPON_CODE_APPLIED, code)
        if (code.isNotBlank()) {
            manualCoupon.set(code)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = buyPageRepo.getCouponFromCode(code, Integer.parseInt(testId), getCompletedLessonCount())
                    val data = response.body()
                    if (response.isSuccessful && data != null && data.couponCode != null) {
                        viewModelScope.launch(Dispatchers.Main) {
                            offersListAdapter.applyCoupon(data)
                            message.what = COUPON_APPLIED
                            message.obj = data
                            message.arg1 = isFromLink
                            message.arg2 = isApplyFrom
                            singleLiveEvent.value = message
                        }
                    } else {
                        showToast("Coupon code is not valid or expired")
                    }
                } catch (e: Exception) {
                    showToast("Oops Something went wrong")
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveImpressionForBuyPageLayout(eventName: String, eventData: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                buyPageRepo.saveBuyPageImpression(
                    mapOf(
                        "event_name" to eventName,
                        "event_data" to eventData
                    )
                )
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                LogException.catchException(ex)
            }
        }
    }

    fun setSupportReason(map : HashMap<String,String>){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val resp = buyPageRepo.postSupportReason(map)
                if (resp.code() == 500)
                    showToast("Session booked we will Call you soon")
                else {
                    if (resp.isSuccessful) {
                        saveImpression("COUNSELOR_SUBMIT_BUTTON")
                        showToast("Call booked! We will Call you soon")
                        viewModelScope.launch(Dispatchers.Main) {
                            message.what = REASON_SUBMITTED_BACK
                            singleLiveEvent.value = message
                        }
                    }
                }
            }catch (ex:Exception){

                Log.e("sagar", "setSupportReason: ${ex.message}")
            }
        }
    }

    fun getSupportReason(){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val resp =  buyPageRepo.getReasonList()
                viewModelScope.launch(Dispatchers.Main) {
                    alreadyReasonSelected = resp.body()?.reasonSelected
                    userPhoneNumber = resp.body()?.phoneNumber
                    message.what = SUPPORT_REASON_LIST
                    message.obj = resp.body()?.reasonsList
                    singleLiveEvent.value = message
                }
            }catch (ex:Exception){
                Log.e("sagar", "setSupportReason: ${ex.message}")
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
                extras["payment_done_from"] = "Buy Page"
                val resp = buyPageRepo.saveBranchLog(extras)
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
                    buyPageRepo.logPaymentEvent(
                        mapOf(
                            "mentor_id" to Mentor.getInstance().getId(),
                            "json" to data
                        )
                    )
            } catch (ex: Exception) {
                ex.printStackTrace()
                LogException.catchException(ex)
            }
        }
    }

}