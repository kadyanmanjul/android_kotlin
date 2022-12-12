package com.joshtalks.joshskills.ui.launch

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
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.notification.NotificationCategory
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.server.signup.LastLoginType
import com.joshtalks.joshskills.util.DeepLinkData
import com.joshtalks.joshskills.util.DeepLinkImpression
import com.joshtalks.joshskills.util.DeepLinkRedirect
import io.branch.referral.Branch
import io.branch.referral.Defines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


const val FETCH_GAID = 1001
const val UPDATE_GAID = 1002
const val FETCH_MENTOR = 1003

//const val REDIRECT = 1004
const val START_ACTIVITY = 1005

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val abTestRepository by lazy { ABTestRepository() }
    var testId: String? = null
    var exploreType: String? = null
    private val launcherScreenImpressionService by lazy {
        AppObjectController.retrofit.create(LauncherScreenImpressionService::class.java)
    }
    val event = EventLiveData

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
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
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
                event.value = Message().apply { what = START_ACTIVITY }
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                LogException.catchException(ex)
            }
        }
    }

    fun initReferral(jsonParams: JSONObject) {
        viewModelScope.launch {
            parseReferralCode(jsonParams)?.let {
                AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME).addBasicParam().addUserDetails()
                    .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId)
                    .addParam(AnalyticsEvent.EXPLORE_TYPE.NAME, exploreType).addParam(
                        AnalyticsEvent.REFERRAL_CODE.NAME, it
                    ).push()
            }
        }
    }

    private fun parseReferralCode(jsonParams: JSONObject) =
        if (jsonParams.has(Defines.Jsonkey.ReferralCode.key)) jsonParams.getString(Defines.Jsonkey.ReferralCode.key)
        else null

    fun addDeepLinkNotificationAnalytics(
        notificationID: String, notificationChannel: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationAnalytics().addAnalytics(
                notificationId = notificationID,
                mEvent = NotificationAnalytics.Action.CLICKED,
                channel = notificationChannel
            )
        }
    }

    fun saveDeepLinkImpression(deepLink: String, action: String) {
        viewModelScope.launch {
            try {


                AppObjectController.commonNetworkService.saveDeepLinkImpression(
                    mapOf(
                        "mentor" to Mentor.getInstance().getId(), "deep_link" to deepLink, "link_action" to action
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun handleBranchAnalytics(jsonParams: JSONObject) {
        testId = jsonParams.getStringOrNull(Defines.Jsonkey.AndroidDeepLinkPath.key)
        exploreType = jsonParams.getStringOrNull(Defines.Jsonkey.ContentType.key)

        //save notification impression
        if (jsonParams.has(DeepLinkData.NOTIFICATION_ID.key) && jsonParams.has(DeepLinkData.NOTIFICATION_CHANNEL.key)) {
            addDeepLinkNotificationAnalytics(
                jsonParams.getString((DeepLinkData.NOTIFICATION_ID.key)),
                jsonParams.getString(DeepLinkData.NOTIFICATION_CHANNEL.key)
            )
        }

        //save referral impression
        if (jsonParams.getStringOrNull(Defines.Jsonkey.UTMMedium.key) == "referral") {
            saveDeepLinkImpression(
                deepLink = jsonParams.getStringOrNull(DeepLinkData.REFERRING_LINK.key) ?: "",
                action = DeepLinkImpression.REFERRAL.name
            )
            updateReferralModel(jsonParams)
            initReferral(jsonParams)
        }

        //save redirect impression
        if (jsonParams.has(DeepLinkData.REDIRECT_TO.key)) {
            if (jsonParams.getString(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.LOGIN.key) {
                PrefManager.put(LOGIN_ONBOARDING, true)
            }
            saveDeepLinkImpression(
                deepLink = jsonParams.getStringOrNull(DeepLinkData.REFERRING_LINK.key) ?: "",
                action = "${DeepLinkImpression.REDIRECT_}${
                    jsonParams.getString(DeepLinkData.REDIRECT_TO.key).uppercase()
                }${
                    when (jsonParams.getString(DeepLinkData.REDIRECT_TO.key)) {
                        DeepLinkRedirect.ONBOARDING.key -> "_${jsonParams.getString(DeepLinkData.COURSE_ID.key)}"
                        DeepLinkRedirect.COURSE_DETAILS.key -> "_${jsonParams.getString(DeepLinkData.TEST_ID.key)}"
                        else -> ""
                    }
                }",
            )
        }
    }

    fun updateReferralModel(jsonParams: JSONObject) {
        (InstallReferrerModel.getPrefObject() ?: InstallReferrerModel()).let {
            if (jsonParams.has(Defines.Jsonkey.ReferralCode.key)) it.utmSource =
                jsonParams.getString(Defines.Jsonkey.ReferralCode.key)
            if (jsonParams.has(Defines.Jsonkey.UTMMedium.key)) it.utmMedium =
                jsonParams.getString(Defines.Jsonkey.UTMMedium.key)

            if (jsonParams.has(Defines.Jsonkey.UTMCampaign.key)) it.utmTerm =
                jsonParams.getString(Defines.Jsonkey.UTMCampaign.key)
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
                NotificationUtils(AppObjectController.joshApplication).updateNotificationDb(NotificationCategory.APP_OPEN)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateABTestCampaigns() =
        abTestRepository.updateAllCampaigns()

    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("gaid", PrefManager.getStringValue(USER_UNIQUE_ID)),
                    Pair("event_name", eventName)
                )
                launcherScreenImpressionService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }
}

fun JSONObject.getStringOrNull(key: String): String? {
    return if (this.has(key)) this.getString(key) else null
}

fun JSONObject.getBooleanOrNull(key: String): Boolean? {
    return if (this.has(key)) this.getBoolean(key) else null
}