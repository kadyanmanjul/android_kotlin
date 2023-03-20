package com.joshtalks.joshskills.ui.inbox

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.errorState.BUY_COURSE_FEATURE_ERROR
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.inbox.adapter.InboxRecommendedAdapter
import com.joshtalks.joshskills.ui.inbox.adapter.InboxRecommendedCourse
import com.joshtalks.joshskills.ui.inbox.payment_verify.Payment
import com.joshtalks.joshskills.ui.inbox.payment_verify.PaymentStatus
import com.joshtalks.joshskills.ui.special_practice.utils.*
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileResponse
import io.branch.referral.util.CurrencyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.HashMap

class InboxViewModel : BaseViewModel(){

    var appDatabase = AppObjectController.appDatabase
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val groupIdLiveData: MutableLiveData<String> = MutableLiveData()
    val paymentStatus: MutableLiveData<Payment> = MutableLiveData()
    val paymentNotInitiated: MutableLiveData<Boolean> = MutableLiveData()

    private val _overAllWatchTime = MutableSharedFlow<Long>(replay = 0)
    val overAllWatchTime: SharedFlow<Long>
        get() = _overAllWatchTime

    private val _registerCourseLocalData = MutableSharedFlow<List<InboxEntity>>(replay = 0)
    val registerCourseLocalData: SharedFlow<List<InboxEntity>>
        get() = _registerCourseLocalData

    val abTestRepository: ABTestRepository by lazy { ABTestRepository() }
    val recommendedAdapter = InboxRecommendedAdapter()
    val isRecommendedVisible = ObservableField(false)

    fun getA2C1CampaignData(campaign: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Utils.isInternetAvailable()) {
                    abTestRepository.getCampaignData(campaign)?.let { campaign ->
                        PrefManager.put(
                            IS_A2_C1_RETENTION_ENABLED,
                            (campaign.variantKey == VariantKeys.A2_C1_RETENTION.name) && campaign.variableMap?.isEnabled == true
                        )
                    } ?: run {
                        AppObjectController.abTestNetworkService.getCampaignData(campaign).let { response ->
                            response.body()?.let { campaign ->
                                PrefManager.put(
                                    IS_A2_C1_RETENTION_ENABLED,
                                    (campaign.variantKey == VariantKeys.A2_C1_RETENTION.name) && campaign.variableMap?.isEnabled == true
                                )
                            }
                        }
                    }
                } else {
                    showToast("No internet connection")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getRegisterCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getCourseFromServer()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getAllRegisterCourseMinimalFromDB() = viewModelScope.launch(Dispatchers.IO) {
        _registerCourseLocalData.emit(appDatabase.courseDao().getRegisterCourseMinimal())
    }

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Utils.isInternetAvailable()) {
                    val courseListResponse = AppObjectController.chatNetworkService.getRegisteredCourses()
                    if (courseListResponse.isEmpty()) {
                        appDatabase.courseDao().deleteAllCourses()
                        _registerCourseLocalData.emit(emptyList())
                        return@launch
                    }
                    val listCourseId = mutableListOf<String>()
                    val dbList = appDatabase.courseDao().getRegisterCourseMinimal()
                    courseListResponse.forEach {
                        listCourseId.add(it.courseId)
                    }
                    val diff = dbList.filterNot { listCourseId.contains(it.courseId) }
                    diff.forEach {
                        appDatabase.courseDao().deleteCourse(it.courseId)
                    }
                    appDatabase.courseDao().insertRegisterCourses(courseListResponse)
                } else {
                    showToast("No internet connection")
                }
                getAllRegisterCourseMinimalFromDB()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    fun getProfileData(mentorId: String) {
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Utils.isInternetAvailable()) {
                    val response = AppObjectController.commonNetworkService.getUserProfileData(
                        mentorId, null,
                        USER_PROFILE_FLOW_FROM.INBOX_SCREEN.value
                    )
                    if (response.isSuccessful && response.body() != null) {
                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                        response.body()?.awardCategory?.sortedBy { it.sortOrder }?.map {
                            it.awards?.sortedBy { it.sortOrder }
                        }
                        if (mentorId == Mentor.getInstance().getId()) {
                            response.body()?.colorCode?.let {
                                PrefManager.put(MY_COLOR_CODE, it, false)
                            }
                        }

                        userData.postValue(response.body()!!)
                        PrefManager.put(
                            IS_PROFILE_FEATURE_ACTIVE,
                            response.body()?.isPointsActive ?: false
                        )
                        return@launch
                    } else if (response.errorBody() != null
                        && response.errorBody()!!.string().contains("mentor_id is not valid")
                    ) {
                        apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    } else {
                        apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    }
                } else {
                    showToast("No internet connection")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            }
        }
        apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
    }


    fun logInboxEngageEvent() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.signUpNetworkService.logInboxEngageEvent(
                    mapOf(
                        "mentor" to Mentor.getInstance().getId()
                    )
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getTotalWatchTime() {
        viewModelScope.launch(Dispatchers.IO) {
            _overAllWatchTime.emit(appDatabase.videoEngageDao().getOverallWatchTime() ?: 0L)
        }
    }

    fun handleGroupTimeTokens() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                GroupRepository().fireTimeTokenAPI()
                GroupRepository().subscribeNotifications()
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    fun handleBroadCastEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            for (event in appDatabase.broadcastDao().getAllEvents()) {
                try {
                    if (Utils.isInternetAvailable()) {
                        val res = AppObjectController.commonNetworkService.saveBroadcastEvent(event)
                        if (res.isSuccessful)
                            appDatabase.broadcastDao().deleteEvent(event.id)
                    } else {
                        showToast("No internet connection")
                    }
                } catch (ex: Exception) {
                    LogException.catchException(ex)
                }
            }
        }
    }

    fun userOnlineStatusSync() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gaid = when {
                    PrefManager.hasKey(USER_UNIQUE_ID, true) -> {
                        PrefManager.getStringValue(USER_UNIQUE_ID, true)
                    }
                    PrefManager.hasKey(USER_UNIQUE_ID, false) -> {
                        PrefManager.getStringValue(USER_UNIQUE_ID, false)
                    }
                    else -> {
                        null
                    }
                }
                PrefManager.put(IS_USER_LOGGED_IN, value = true, isConsistent = true)
                val response = AppObjectController.signUpNetworkService.userActive(
                    Mentor.getInstance().getId(),
                    mapOf("gaid" to gaid, "device_id" to Utils.getDeviceId())
                )
                if (response.isSuccessful && response.body()?.isLatestLoginDevice == false) {
                    Mentor.deleteUserCredentials(true)
                    Mentor.deleteUserData()
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    fun checkForPendingPayments() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lastPaymentEntry = appDatabase.paymentDao().getLastPaymentEntry()
                if (lastPaymentEntry != null && lastPaymentEntry.isdeleted.not()) {
                    if (lastPaymentEntry.isSync && (lastPaymentEntry.status == PaymentStatus.SUCCESS || lastPaymentEntry.status == PaymentStatus.FAILED)) {
                        when (lastPaymentEntry.status) {
                            PaymentStatus.SUCCESS -> {
                                if (PrefManager.getBoolValue(IS_APP_RESTARTED, false)) {
                                    appDatabase.paymentDao().deletePaymentEntry(lastPaymentEntry.razorpayOrderId)
                                } else {
                                    paymentStatus.postValue(lastPaymentEntry)
                                }
                            }
                            PaymentStatus.FAILED -> {
                                if (lastPaymentEntry.timeStamp.plus(1000 * 60 * 60 * 4) < System.currentTimeMillis()) {
                                    appDatabase.paymentDao()
                                        .deletePaymentEntry(lastPaymentEntry.razorpayOrderId)
                                } else {
                                    paymentStatus.postValue(lastPaymentEntry)
                                }
                            }
                            else -> {

                            }
                        }
                    } else {
                        val res =
                            AppObjectController.commonNetworkService.syncPaymentStatus(lastPaymentEntry.razorpayKeyId)
                        val response = res.body()?.toString()
                        lastPaymentEntry.response = response.toString()
                        appDatabase.paymentDao()
                            .updatePayment(lastPaymentEntry)

                        if (res.isSuccessful && res.body() != null) {
                            if (res.body()!!.payment == null) {
                                /* appDatabase.paymentDao()
                                     .deletePaymentEntry(it.razorpayOrderId)*/
                            } else {
                                appDatabase.paymentDao()
                                    .updatePaymentStatus(lastPaymentEntry.razorpayOrderId, res.body()!!.payment!!)
                                lastPaymentEntry.status = res.body()!!.payment
                                paymentStatus.postValue(lastPaymentEntry)
                            }
                        }
                    }
                } else {
                    paymentNotInitiated.postValue(true)
                }
            } catch (ex: Exception) {

            }
        }
    }

    suspend fun syncPaymentStatus(razorpayOrderId: String, status: PaymentStatus) {
        appDatabase.paymentDao().updatePaymentStatus(razorpayOrderId, status)
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
    fun saveNotificationPermissionGrantStatus(status: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val value = if(status) 1 else 0
                val requestData = hashMapOf(
                    Pair("device_id", Utils.getDeviceId()),
                    Pair("is_allowed", value.toString())
                )
                AppObjectController.commonNetworkService.saveImpressionForNotification(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun saveNotificationEnabledStatus(status: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val value = if(status) 1 else 0
                val requestData = hashMapOf(
                    Pair("is_enabled", value.toString())
                )
                AppObjectController.commonNetworkService.saveImpressionForNotificationStatus(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun getRecommendedCourse(){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = AppObjectController.commonNetworkService.getCourseRecommendations()
                withContext(Dispatchers.Main) {
                    isRecommendedVisible.set(true)
                    recommendedAdapter.addRecommendedCourseList(resp.body())
                }
            } catch (ex: Exception) {
                Log.e("sagar", "setSupportReason: ${ex.message}")
            }
        }
    }

    val onItemClick: (InboxRecommendedCourse, Int, Int) -> Unit = { it, type, position ->
        try {
            when (type) {
                CLICK_ON_RECOMMENDED_COURSE ->{
                    savePaymentImpressionForCourseExplorePage("CLICKED_COURSE_INBOX",it.id.toString())
                    message.what = CLICK_ON_RECOMMENDED_COURSE
                    message.obj = it
                    message.arg1 = position
                    singleLiveEvent.value = message
                }
            }
        } catch (ex: Exception) {
            Log.d("BuyPageViewModel.kt", "SAGAR => :145 ${ex.message}")
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
    fun saveBranchPaymentLog(orderInfoId:String,
                             amount: BigDecimal?,
                             testId: Int = 0,
                             courseName: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extras: HashMap<String, Any> = HashMap()
                extras["test_id"] = testId
                extras["orderinfo_id"] = orderInfoId
                extras["currency"] = CurrencyType.INR.name
                extras["amount"] = amount ?: 0.0
                extras["course_name"] = courseName
                extras["device_id"] = Utils.getDeviceId()
                extras["guest_mentor_id"] = Mentor.getInstance().getId()
                extras["payment_done_from"] = "Inbox Screen"
                val resp = AppObjectController.commonNetworkService.savePaymentLog(extras)
            } catch (ex: Exception) {
                Log.e("sagar", "setSupportReason: ${ex.message}")
            }
        }
    }

    fun getBuyPageFeature() {
        AppObjectController.applicationLevelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getCourseFeatureDetailsV2()
                if (response.isSuccessful) {
                    AppObjectController.appDatabase.getCourseFeatureDataDao().insertBuyCourseFeatureData(response.body())
                }else{
                    withContext(Dispatchers.Main) {
                        sendErrorMessage(response.code().toString(), null, BUY_COURSE_FEATURE_ERROR)
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    sendErrorMessage(ex.message.toString(), null, BUY_COURSE_FEATURE_ERROR)
                }
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
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
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

    fun savePreviousNotificationImpressions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val days = PrefManager.getIntValue(INACTIVE_DAYS, false, 0)
                PrefManager.put(INACTIVE_DAYS,0,false)
                val numNotifications = PrefManager.getIntValue(TOTAL_LOCAL_NOTIFICATIONS_SHOWN, false, 0)
                if (days > 0 && numNotifications > 0) {
                    val requestData = hashMapOf(
                        Pair("mentor_id", Mentor.getInstance().getId()),
                        Pair("event_name", "APP_OPENED_AFTER_DROP"),
                        Pair("number_of_inactive_days", days),
                        Pair("total_notifications_sent", numNotifications)
                    )
                    AppObjectController.commonNetworkService.saveImpressionForClientNotifications(
                        requestData
                    )
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

}