package com.joshtalks.joshskills.ui.launch

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.server.signup.LastLoginType
import com.joshtalks.joshskills.ui.inbox.mentor_id
import com.joshtalks.joshskills.util.DeepLinkImpression
import com.joshtalks.joshskills.util.DeepLinkUtil
import com.singular.sdk.Singular
import io.branch.referral.Branch
import io.branch.referral.Defines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun logAppLaunchEvent(networkOperatorName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.NETWORK_CARRIER.NAME, networkOperatorName)
                .push()
        }
    }

    fun addDeepLinkNotificationAnalytics(
        notificationID: String,
        notificationChannel: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationAnalytics().addAnalytics(
                notificationId = notificationID,
                mEvent = NotificationAnalytics.Action.CLICKED,
                channel = notificationChannel
            )
        }
    }

    fun initReferral(
        testId: String? = null,
        exploreType: String? = null,
        jsonParams: JSONObject
    ) {
        viewModelScope.launch {
            parseReferralCode(jsonParams)?.let {
                AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId)
                    .addParam(AnalyticsEvent.EXPLORE_TYPE.NAME, exploreType)
                    .addParam(
                        AnalyticsEvent.REFERRAL_CODE.NAME,
                        it
                    )
                    .push()
            }
        }
    }

    fun initGaid(
        testId: String? = null,
        exploreType: String? = null
    ) {
        Log.d(TAG, "initGaid() called with: testId = $testId, exploreType = $exploreType")
        if (apiCallStatus.value == ApiCallStatus.START) {
            return
        }
        apiCallStatus.postValue(ApiCallStatus.START)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        viewModelScope.launch {
            val obj = RequestRegisterGAId()
            try {
                obj.test = testId?.split("_")?.get(1)?.toInt()
                if (PrefManager.getStringValue(USER_UNIQUE_ID).isBlank()) {
                    PrefManager.put(USER_UNIQUE_ID, EMPTY)
                    val response =
                        AppObjectController.signUpNetworkService.getGaid(mapOf("device_id" to Utils.getDeviceId()))
                    if (response.isSuccessful && response.body() != null) {
                        PrefManager.put(USER_UNIQUE_ID, response.body()!!.gaID)
                    } else {
                        apiCallStatus.postValue(ApiCallStatus.FAILED)
                        return@launch
                    }
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                return@launch
            }

            obj.gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
            InstallReferrerModel.getPrefObject()?.let {
                obj.installOn = it.installOn
                obj.utmMedium =
                    if (it.utmMedium.isNullOrEmpty() &&
                        it.otherInfo != null &&
                        it.otherInfo!!.containsKey("utm_medium")
                    ) it.otherInfo!!["utm_medium"]
                    else
                        it.utmMedium
                obj.utmSource =
                    if (it.utmSource.isNullOrEmpty() &&
                        it.otherInfo != null &&
                        it.otherInfo!!.containsKey("utm_source")
                    ) it.otherInfo!!["utm_source"]
                    else it.utmSource
                obj.utmTerm =
                    if (it.utmTerm.isNullOrEmpty() &&
                        it.otherInfo != null &&
                        it.otherInfo!!.containsKey("utm_campaign")
                    ) it.otherInfo!!["utm_campaign"]
                    else it.utmTerm
            }

            if (exploreType.isNullOrEmpty().not()) {
                obj.exploreCardType = ExploreCardType.valueOf(exploreType!!)
            }
            try {
                val resp = AppObjectController.commonNetworkService.registerGAIdDetailsV2Async(obj)
                GaIDMentorModel.update(resp)
                PrefManager.put(SERVER_GID_ID, resp.gaidServerDbId)
                PrefManager.put(EXPLORE_TYPE, exploreType ?: ExploreCardType.NORMAL.name, false)
//                PrefManager.put(INSTANCE_ID, resp.instanceId)
//                PrefManager.put(INSTANCE_ID, resp.instanceId, isConsistent = true)
                getMentorForUser(PrefManager.getStringValue(USER_UNIQUE_ID), testId)
            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    fun getMentorForUser(gaid: String, testId: String?) {
        viewModelScope.launch {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response =
                    AppObjectController.signUpNetworkService.createGuestUser(mapOf("gaid" to gaid))
                if (response.lastLoginType != LastLoginType.NEVER)
                    PrefManager.put(LAST_LOGIN_TYPE, response.lastLoginType.name)
                Mentor.updateFromLoginResponse(response)
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                LogException.catchException(ex)
            }
        }
    }

    private fun parseReferralCode(jsonParams: JSONObject) =
        if (jsonParams.has(Defines.Jsonkey.ReferralCode.key))
            jsonParams.getString(Defines.Jsonkey.ReferralCode.key)
        else null

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

    fun updateReferralModel(jsonParams: JSONObject) {
        (InstallReferrerModel.getPrefObject() ?: InstallReferrerModel()).let {
            if (jsonParams.has(Defines.Jsonkey.ReferralCode.key))
                it.utmSource =
                    jsonParams.getString(Defines.Jsonkey.ReferralCode.key)
            if (jsonParams.has(Defines.Jsonkey.UTMMedium.key))
                it.utmMedium =
                    jsonParams.getString(Defines.Jsonkey.UTMMedium.key)

            if (jsonParams.has(Defines.Jsonkey.UTMCampaign.key))
                it.utmTerm =
                    jsonParams.getString(Defines.Jsonkey.UTMCampaign.key)
            InstallReferrerModel.update(it)
        }
    }

    fun saveDeepLinkImpression(deepLink: String, action: String) {
        viewModelScope.launch {
            try {
                val response = AppObjectController.commonNetworkService.saveDeepLinkImpression(
                    mapOf(
                        "mentor" to Mentor.getInstance().getId(),
                        "deep_link" to deepLink,
                        "link_action" to action
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addAnalytics() {
        val isAppOpenedForFirstTime = PrefManager.getBoolValue(IS_APP_OPENED_FOR_FIRST_TIME, true, true)
        MixPanelTracker.mixPanel.track("app session")
        if (isAppOpenedForFirstTime) {
            PrefManager.put(IS_APP_OPENED_FOR_FIRST_TIME, value = false, isConsistent = true)
            MixPanelTracker.publishEvent(MixPanelEvent.APP_OPENED_FOR_FIRST_TIME).push()
            Singular.event(SingularEvent.APP_OPENED_FIRST_TIME.value)
        }
    }
}