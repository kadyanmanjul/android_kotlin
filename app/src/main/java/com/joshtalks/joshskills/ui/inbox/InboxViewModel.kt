package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.notification.NotificationCategory
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.inbox.payment_verify.Payment
import com.joshtalks.joshskills.ui.inbox.payment_verify.PaymentStatus
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileResponse
import com.xiaomi.push.ex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.launch
import org.json.JSONObject

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val groupIdLiveData: MutableLiveData<String> = MutableLiveData()
    val paymentStatus: MutableLiveData<Payment> = MutableLiveData()

    private val _overAllWatchTime = MutableSharedFlow<Long>(replay = 0)
    val overAllWatchTime: SharedFlow<Long>
        get() = _overAllWatchTime

    private val _registerCourseLocalData = MutableSharedFlow<List<InboxEntity>>(replay = 0)
    val registerCourseLocalData: SharedFlow<List<InboxEntity>>
        get() = _registerCourseLocalData

    val abTestRepository: ABTestRepository by lazy { ABTestRepository() }

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
                    val courseListResponse =
                        AppObjectController.chatNetworkService.getRegisteredCourses()
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
                        MixPanelTracker.mixPanel.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
                        MixPanelTracker.mixPanel.people.identify(
                            PrefManager.getStringValue(
                                USER_UNIQUE_ID
                            )
                        )
                        val prop = JSONObject()
                        prop.put("total points", response.body()?.points)
                        prop.put("total min spoken", response.body()?.minutesSpoken)
                        MixPanelTracker.mixPanel.people.set(prop)
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

    fun initializeMoEngageUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.groupsNetworkService.createMoEngageUser(
                    Mentor.getInstance().getId()
                )
            } catch (ex: Exception) {
                LogException.catchException(ex)
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
                            AppObjectController.commonNetworkService.syncPaymentStatus(lastPaymentEntry.razorpayOrderId)
                        val response = res.body()?.toString()
                        lastPaymentEntry.response = lastPaymentEntry.response.plus(response)
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
                }
            } catch (ex: Exception) {

            }
        }
    }

    suspend fun syncPaymentStatus(razorpayOrderId: String, status: PaymentStatus) {
        appDatabase.paymentDao().updatePaymentStatus(razorpayOrderId, status)
    }

    fun getFreeTrialNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.utilsAPIService.getFTScheduledNotifications(
                    PrefManager.getStringValue(
                        FREE_TRIAL_TEST_ID,
                        false,
                        FREE_TRIAL_DEFAULT_TEST_ID
                    )
                )
                AppObjectController.appDatabase.scheduleNotificationDao().insertAllNotifications(response)
                if (response.isNotEmpty())
                    PrefManager.put(FETCHED_SCHEDULED_NOTIFICATION, true)
                NotificationUtils(context).removeScheduledNotification(NotificationCategory.APP_OPEN)
                NotificationUtils(context).updateNotificationDb(NotificationCategory.AFTER_LOGIN)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}