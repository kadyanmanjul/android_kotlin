package com.joshtalks.joshskills

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Message
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.joshtalks.joshskills.common.base.EventLiveData
import com.joshtalks.joshskills.common.constants.FETCH_MENTOR
import com.joshtalks.joshskills.common.constants.START_ACTIVITY
import com.joshtalks.joshskills.common.constants.UPDATE_GAID
import com.joshtalks.joshskills.common.core.ApiCallStatus
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.analytics.LogException
import com.joshtalks.joshskills.common.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.notification.NotificationCategory
import com.joshtalks.joshskills.common.core.notification.client_side.ClientNotificationUtils
import com.joshtalks.joshskills.common.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.common.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.common.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.common.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.common.repository.local.model.User
import com.joshtalks.joshskills.common.repository.server.signup.LastLoginType
import com.joshtalks.joshskills.deeplink.DeepLinkData
import com.joshtalks.joshskills.deeplink.DeepLinkRedirect
import com.joshtalks.joshskills.deeplink.DeepLinkRedirectUtil
import com.joshtalks.joshskills.deeplink.RedirectAction
import io.branch.referral.Branch
import io.branch.referral.Defines
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    var jsonParams: JSONObject = JSONObject()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val abTestRepository by lazy { ABTestRepository() }
    var testId: String? = null
    var exploreType: String? = null
    val deepLinkRedirectUtil by lazy { DeepLinkRedirectUtil(jsonParams) }
    private val launcherScreenImpressionService by lazy {
        AppObjectController.retrofit.create(LauncherScreenImpressionService::class.java)
    }
    val event = EventLiveData
    val redirectEvent: MutableLiveData<RedirectAction> = MutableLiveData()

    fun initApp() {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(AppObjectController.joshApplication).cancelAllWork()
            WorkManagerAdmin.appInitWorker()
            val dateFormat = SimpleDateFormat("HH")
            val time: Int = dateFormat.format(Date()).toInt()
            val getCurrentTimeInMillis = Calendar.getInstance().timeInMillis
            val lastFakeCallInMillis: Long =
                PrefManager.getLongValue(LAST_FAKE_CALL_INVOKE_TIME, true)
            if ((time in 7..23) && isUserOnline(getApplication<Application>().applicationContext) && getCurrentTimeInMillis - lastFakeCallInMillis >= 3600000) {
                PrefManager.put(LAST_FAKE_CALL_INVOKE_TIME, getCurrentTimeInMillis, true)
                WorkManagerAdmin.setFakeCallNotificationWorker()
            }
            Branch.getAutoInstance(AppObjectController.joshApplication).resetUserSession()
            logAppLaunchEvent(getNetworkOperatorName())
            if (PrefManager.hasKey(IS_FREE_TRIAL).not() && User.getInstance().isVerified.not()) {
                PrefManager.put(IS_FREE_TRIAL, value = true, isConsistent = false)
            }
            PrefManager.put(IS_APP_RESTARTED, true)
        }
    }

    private fun getNetworkOperatorName() =
        (AppObjectController.joshApplication.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperatorName
            ?: ""

    private fun isUserOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun logAppLaunchEvent(networkOperatorName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.NETWORK_CARRIER.NAME, networkOperatorName)
                .push()
        }
    }

    fun getGaid() {
        apiCallStatus.postValue(ApiCallStatus.START)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        viewModelScope.launch {
            try {
                val response =
                    AppObjectController.signUpNetworkService.getGaid(mapOf("device_id" to Utils.getDeviceId()))
                if (response.isSuccessful && response.body() != null) {
                    PrefManager.put(USER_UNIQUE_ID, response.body()!!.gaID)
                    saveImpression("GAID_CREATED")
                    Branch.getInstance().setIdentity(response.body()!!.gaID)
                    event.value = Message().apply { what = UPDATE_GAID }
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                return@launch
            }
        }
    }

    fun updateGaid() {
        viewModelScope.launch {
            apiCallStatus.postValue(ApiCallStatus.START)
            val requestRegisterGAId = RequestRegisterGAId()
            requestRegisterGAId.test = testId?.split("_")?.getOrNull(1)?.toInt()
            requestRegisterGAId.gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
            InstallReferrerModel.getPrefObject()?.let {
                requestRegisterGAId.installOn = it.installOn
                if (it.otherInfo != null) {
                    requestRegisterGAId.utmMedium =
                        if (it.utmMedium.isNullOrEmpty() && it.otherInfo!!.containsKey("utm_medium")) {
                            it.otherInfo!!["utm_medium"]
                        } else
                            it.utmMedium
                    requestRegisterGAId.utmSource =
                        if (it.utmSource.isNullOrEmpty() && it.otherInfo!!.containsKey("utm_source"))
                            it.otherInfo!!["utm_source"]
                        else
                            it.utmSource
                    requestRegisterGAId.utmTerm =
                        if (it.utmTerm.isNullOrEmpty() && it.otherInfo!!.containsKey("utm_campaign"))
                            it.otherInfo!!["utm_campaign"]
                        else
                            it.utmTerm
                }
            }

            if (exploreType.isNullOrEmpty().not()) {
                requestRegisterGAId.exploreCardType = ExploreCardType.valueOf(exploreType!!)
            }
            try {
                val resp = AppObjectController.commonNetworkService.registerGAIdDetailsV2Async(requestRegisterGAId)
                saveImpression("GAID_INFO_CREATED")
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                GaIDMentorModel.update(resp)
                PrefManager.put(SERVER_GID_ID, resp.gaidServerDbId)
                PrefManager.put(EXPLORE_TYPE, exploreType ?: ExploreCardType.NORMAL.name, false)
                event.value = Message().apply { what = FETCH_MENTOR }
            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
                LogException.catchException(ex)
            }
        }
    }

    fun getGuestMentor() {
        viewModelScope.launch {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response = AppObjectController.signUpNetworkService.createGuestUser(
                    mapOf(
                        "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID)
                    )
                )
                saveImpression("MENTOR_CREATED")
                if (response.lastLoginType != LastLoginType.NEVER)
                    PrefManager.put(LAST_LOGIN_TYPE, response.lastLoginType.name)
                Mentor.updateFromLoginResponse(response)
                delay(700)
                event.value = Message().apply { what = START_ACTIVITY }
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                LogException.catchException(ex)
            }
        }
    }

    fun handleBranchAnalytics() {
        testId = jsonParams.getStringOrNull(Defines.Jsonkey.AndroidDeepLinkPath.key)
        exploreType = jsonParams.getStringOrNull(Defines.Jsonkey.ContentType.key)
        viewModelScope.launch {
            deepLinkRedirectUtil.handleBranchAnalytics()
            if (deepLinkRedirectUtil.isReferralLink()) {
                initReferral()
            }
        }
    }

    private suspend fun initReferral() {
        parseReferralCode()?.let {
            updateReferralModel()
            AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId)
                .addParam(AnalyticsEvent.EXPLORE_TYPE.NAME, exploreType)
                .addParam(AnalyticsEvent.REFERRAL_CODE.NAME, it)
                .push()
        }
    }

    private fun parseReferralCode() =
        jsonParams.getStringOrNull(Defines.Jsonkey.ReferralCode.key)

    private fun updateReferralModel() {
        (InstallReferrerModel.getPrefObject() ?: InstallReferrerModel()).let {
            if (jsonParams.has(Defines.Jsonkey.ReferralCode.key))
                it.utmSource = jsonParams.getString(Defines.Jsonkey.ReferralCode.key)
            if (jsonParams.has(Defines.Jsonkey.UTMMedium.key))
                it.utmMedium = jsonParams.getString(Defines.Jsonkey.UTMMedium.key)
            if (jsonParams.has(Defines.Jsonkey.UTMCampaign.key))
                it.utmTerm = jsonParams.getString(Defines.Jsonkey.UTMCampaign.key)
            InstallReferrerModel.update(it)
        }
    }

    fun canRunApplication(): Boolean {
        val path = getApplication<Application>().filesDir.absolutePath
        val count = getDotCount(path)
        return count <= APP_PACKAGE_COUNT
    }

    private fun getDotCount(path: String): Int {
        var count = 0
        for (element in path) {
            if (count > APP_PACKAGE_COUNT) {
                break
            }
            if (element == '.') {
                count++
            }
        }
        return count
    }

    fun addAnalytics() {
        val isAppOpenedForFirstTime = PrefManager.getBoolValue(IS_APP_OPENED_FOR_FIRST_TIME, true, true)
        //MixPanelTracker.mixPanel.track("app session")
        if (isAppOpenedForFirstTime) {
            PrefManager.put(IS_APP_OPENED_FOR_FIRST_TIME, value = false, isConsistent = true)
            MixPanelTracker.publishEvent(MixPanelEvent.APP_OPENED_FOR_FIRST_TIME).push()
            MarketingAnalytics.openAppFirstTime()
            getAppOpenNotifications()
        }
    }

    private fun getAppOpenNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.utilsAPIService.getFTScheduledNotifications()
                AppObjectController.appDatabase.scheduleNotificationDao().insertAllNotifications(response)
                ClientNotificationUtils(AppObjectController.joshApplication).updateNotificationDb(NotificationCategory.APP_OPEN)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateABTestCampaigns() = abTestRepository.updateAllCampaigns()

    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("gaid", PrefManager.getStringValue(USER_UNIQUE_ID)), Pair("event_name", eventName)
                )
                launcherScreenImpressionService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun initDeepLinkData() {
        if (jsonParams.getBooleanOrNull(Defines.Jsonkey.Clicked_Branch_Link.key) == true) {
            handleBranchAnalytics()
        }
        if (jsonParams.getStringOrNull(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.LOGIN.key) {
            PrefManager.put(LOGIN_ONBOARDING, true)
        }
    }

    fun redirectToActivity(activity: CoreJoshActivity, userProfileNotComplete: Boolean) {
        viewModelScope.launch {
            redirectEvent.postValue(
                when {
                    deepLinkRedirectUtil.isRedirectLink() -> deepLinkRedirectUtil.handleDeepLink(
                        activity,
                        userProfileNotComplete
                    )
                    User.getInstance().isVerified.not() -> getUnverifiedUserRedirectAction()
                    userProfileNotComplete -> RedirectAction.SIGN_UP
                    else -> RedirectAction.INBOX
                }
            )
        }
    }

    private fun getUnverifiedUserRedirectAction(): RedirectAction =
        when {
            isPaymentDone() -> RedirectAction.SIGN_UP
            isGuestEnrolled() -> RedirectAction.INBOX
            else -> RedirectAction.COURSE_ONBOARDING
        }

    private fun isPaymentDone(): Boolean =
        PrefManager.getBoolValue(IS_PAYMENT_DONE, false)

    private fun isFreeTrialStarted(): Boolean =
        PrefManager.getBoolValue(IS_FREE_TRIAL, isConsistent = false, defValue = false)

    private fun isGuestEnrolled(): Boolean =
        PrefManager.getBoolValue(IS_GUEST_ENROLLED, false)
}

fun JSONObject.getStringOrNull(key: String): String? {
    return if (this.has(key)) this.getString(key) else null
}

fun JSONObject.getBooleanOrNull(key: String): Boolean? {
    return if (this.has(key)) this.getBoolean(key) else null
}