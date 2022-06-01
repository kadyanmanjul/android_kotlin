package com.joshtalks.joshskills.ui.launch

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.START_SERVICE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.notification.HAS_LOCAL_NOTIFICATION
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.ui.call.CallingServiceReceiver
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.util.DeepLinkData
import com.joshtalks.joshskills.util.DeepLinkRedirectUtil
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import kotlinx.android.synthetic.main.activity_launcher.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "LauncherActivity"

class LauncherActivity : CoreJoshActivity(), Branch.BranchReferralInitListener {
    private var testId: String? = null
    private val apiRun: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        initApp()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        LogSaver.startSavingLog() // to save logs in external storage
        animatedProgressBar()
        initAppInFirstTime()
        handleIntent()
        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
    }

    private fun initApp() {
        lifecycleScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(applicationContext).cancelAllWork()
            WorkManagerAdmin.appInitWorker()
            val dateFormat = SimpleDateFormat("HH")
            val time: Int = dateFormat.format(Date()).toInt()
            val getCurrentTimeInMillis = Calendar.getInstance().timeInMillis
            var lastFakeCallInMillis: Long =
                PrefManager.getLongValue(LAST_FAKE_CALL_INVOKE_TIME, true)
            if ((time in 7..23) && isUserOnline(this@LauncherActivity) && getCurrentTimeInMillis - lastFakeCallInMillis >= 3600000) {
                PrefManager.put(LAST_FAKE_CALL_INVOKE_TIME, getCurrentTimeInMillis, true)
                WorkManagerAdmin.setFakeCallNotificationWorker()
            }
            Branch.getAutoInstance(AppObjectController.joshApplication).resetUserSession()
            logAppLaunchEvent(getNetworkOperatorName())
            if (PrefManager.hasKey(IS_FREE_TRIAL).not() && User.getInstance().isVerified.not()) {
                PrefManager.put(IS_FREE_TRIAL, true, false)
            }
        }

        var isAppOpenedForFirstTime = PrefManager.getBoolValue(IS_APP_OPENED_FOR_FIRST_TIME, true)
        MixPanelTracker.mixPanel.track("app session")
        if (isAppOpenedForFirstTime) {
            PrefManager.put(IS_APP_OPENED_FOR_FIRST_TIME, false, true)
            MixPanelTracker.publishEvent(MixPanelEvent.APP_OPENED_FOR_FIRST_TIME).push()
        }
    }

    fun isUserOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
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
        }
        return false
    }

    private fun animatedProgressBar() {
        val backgroundColorAnimator: ObjectAnimator = ObjectAnimator.ofObject(
            progress_bar, "backgroundColor", ArgbEvaluator(), -0x1, -0x873a07
        )
        backgroundColorAnimator.duration = 300
        backgroundColorAnimator.start()
        retry.setOnClickListener {
            analyzeAppRequirement()
            retry.visibility = View.INVISIBLE
        }
    }

    private fun analyzeAppRequirement() {
        lifecycleScope.launch(Dispatchers.IO) {
            when {
                PrefManager.getStringValue(INSTANCE_ID).isEmpty() -> {
                    if (intent.data == null)
                        initGaid(testId)
                }
                Mentor.getInstance().hasId() -> {
                    startNextActivity()
                }
                else -> {
                    getMentorForUser(PrefManager.getStringValue(INSTANCE_ID), testId)
                }
            }
        }
    }

    private fun handleIntent() {
        if (intent.hasExtra(HAS_LOCAL_NOTIFICATION) && intent.getBooleanExtra(
                HAS_LOCAL_NOTIFICATION,
                false
            )
        ) {
            PrefManager.put(HAS_SEEN_LOCAL_NOTIFICATION, true)
            PrefManager.put(
                LOCAL_NOTIFICATION_INDEX,
                PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX, defValue = 0).plus(1)
            )
        }
    }

    private fun addDeepLinkNotificationAnalytics(
        notificationID: String,
        notificationChannel: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            NotificationAnalytics().addAnalytics(
                notificationId = notificationID,
                mEvent = NotificationAnalytics.Action.CLICKED,
                channel = notificationChannel
            )
        }
    }

    private fun initReferral(
        testId: String? = null,
        exploreType: String? = null,
        jsonParams: JSONObject
    ) {
        lifecycleScope.launchWhenCreated {
            parseReferralCode(jsonParams)?.let {
                logInstallByReferralEvent(testId, exploreType, it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        if (intent.hasExtra("branch_force_new_session") && intent.getBooleanExtra(
                "branch_force_new_session",
                false
            )
        )
            Branch.sessionBuilder(this).withCallback(this@LauncherActivity).reInit()
        handleIntent()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this@LauncherActivity)
            .registerReceiver(CallingServiceReceiver(), IntentFilter(CALLING_SERVICE_ACTION))
        val isCourseBought = PrefManager.getBoolValue(IS_COURSE_BOUGHT, false)
        val courseExpiryTime =
            PrefManager.getLongValue(com.joshtalks.joshskills.core.COURSE_EXPIRY_TIME_IN_MS)
        if ((isCourseBought && User.getInstance().isVerified) || courseExpiryTime != 0L &&
            courseExpiryTime >= System.currentTimeMillis()
        ) {
            val broadcastIntent = Intent().apply {
                action = CALLING_SERVICE_ACTION
                putExtra(SERVICE_BROADCAST_KEY, START_SERVICE)
            }
            LocalBroadcastManager.getInstance(this@LauncherActivity).sendBroadcast(broadcastIntent)
        }
        Branch.sessionBuilder(this)
            .withCallback(this@LauncherActivity)
            .withData(this.intent?.data)
            .init()
    }

    override fun onStop() {
        super.onStop()
        Branch.sessionBuilder(null)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        super.onBackPressed()
        this.finishAndRemoveTask()
    }

    private fun startNextActivity(jsonParams: JSONObject? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            WorkManagerAdmin.appStartWorker()
            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
            Log.d(TAG, "YASH => startNextActivity() called $jsonParams")
            when {
                User.getInstance().isVerified.not() -> {
                    when {
                        (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false) &&
                                PrefManager.getBoolValue(IS_PAYMENT_DONE, false).not()) -> {

                            getInboxActivityIntent()
                        }
                        PrefManager.getBoolValue(IS_PAYMENT_DONE, false) -> {
                            startActivity(Intent(this@LauncherActivity, SignUpActivity::class.java))
                        }
                        PrefManager.getBoolValue(IS_FREE_TRIAL, false, false) -> {
                            startActivity(
                                Intent(
                                    this@LauncherActivity,
                                    FreeTrialOnBoardActivity::class.java
                                )
                            )
                        }
                        else -> {
                            startActivity(Intent(this@LauncherActivity, SignUpActivity::class.java))
                        }
                    }
                }
                isUserProfileNotComplete() -> {
                    startActivity(Intent(this@LauncherActivity, SignUpActivity::class.java))
                }
                containsFavUserCallBackUrl() -> {
                    startActivity(getWebRtcActivityIntent())
                }
                jsonParams != null -> DeepLinkRedirectUtil.getIntent(
                    this@LauncherActivity,
                    jsonParams
                ) ?: startActivity(getInboxActivityIntent())
                else -> startActivity(getInboxActivityIntent())
            }
            this@LauncherActivity.finishAndRemoveTask()
        }
    }

    private fun logInstallByReferralEvent(
        testId: String?,
        exploreType: String?,
        referralCode: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId)
                .addParam(AnalyticsEvent.EXPLORE_TYPE.NAME, exploreType)
                .addParam(
                    AnalyticsEvent.REFERRAL_CODE.NAME,
                    referralCode
                )
                .push()
        }
    }

    private fun logAppLaunchEvent(networkOperatorName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.NETWORK_CARRIER.NAME, networkOperatorName)
                .push()
        }
    }

    private fun getNetworkOperatorName() =
        (baseContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperatorName
            ?: ""

    private fun parseReferralCode(jsonParams: JSONObject) =
        if (jsonParams.has(Defines.Jsonkey.ReferralCode.key))
            jsonParams.getString(Defines.Jsonkey.ReferralCode.key)
        else null

    private fun initAppInFirstTime() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (Utils.isInternetAvailable().not() && PrefManager.hasKey(SERVER_GID_ID)) {
                startNextActivity()
            }
        }
    }

    private fun initAfterBranch(
        testId: String? = null,
        exploreType: String? = null,
        jsonParams: JSONObject? = null
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            when {
                testId != null -> {
                    initGaid(testId, exploreType)
                }
                PrefManager.hasKey(SERVER_GID_ID) -> {
                    if (PrefManager.hasKey(API_TOKEN)) {
                        startNextActivity(jsonParams)
                    } else {
                        getMentorForUser(PrefManager.getStringValue(INSTANCE_ID), testId)
                    }
                }
                Mentor.getInstance().hasId() -> {
                    startNextActivity(jsonParams)
                }
                else -> {
                    initGaid(testId)
                }
            }
        }
    }

    private fun navigateToCourseDetailsScreen(testId: String) {
        WorkManagerAdmin.appStartWorker()
        try {
            CourseDetailsActivity.startCourseDetailsActivity(
                this,
                testId.split("_")[1].toInt(),
                this@LauncherActivity.javaClass.simpleName,
                buySubscription = false
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        this@LauncherActivity.finish()
    }

    private fun navigateToNextScreen() {
        startNextActivity()
    }

    private fun initGaid(
        testId: String? = null,
        exploreType: String? = null
    ) {
        if (apiRun.get()) {
            return
        }
        apiRun.set(true)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        this.testId = testId
        lifecycleScope.launch(Dispatchers.IO) {
            val obj = RequestRegisterGAId()
            try {
                obj.test = testId?.split("_")?.get(1)?.toInt()
            } catch (ex: Throwable) {
            }
            try {
                if (PrefManager.getStringValue(USER_UNIQUE_ID).isBlank()) {
                    PrefManager.put(USER_UNIQUE_ID, EMPTY)
                    val response =
                        AppObjectController.signUpNetworkService.getGaid(mapOf("device_id" to Utils.getDeviceId()))
                    if (response.isSuccessful && response.body() != null) {
                        PrefManager.put(USER_UNIQUE_ID, response.body()!!.gaID)
                    } else {
                        return@launch
                    }
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
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
                Log.i(TAG, "initGaid: registerGAIdDetailsV2Async => $obj")
                val resp = AppObjectController.commonNetworkService.registerGAIdDetailsV2Async(obj)
                GaIDMentorModel.update(resp)
                PrefManager.put(SERVER_GID_ID, resp.gaidServerDbId)
                PrefManager.put(EXPLORE_TYPE, exploreType ?: ExploreCardType.NORMAL.name, false)
                PrefManager.put(INSTANCE_ID, resp.instanceId)
                PrefManager.put(INSTANCE_ID, resp.instanceId, isConsistent = true)
                getMentorForUser(resp.instanceId, testId)
            } catch (ex: Exception) {
                apiRun.set(false)
                AppObjectController.uiHandler.post {
                    retry.visibility = View.VISIBLE
                }
                ex.printStackTrace()
            }
        }
    }

    private fun getMentorForUser(instanceId: String, testId: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.createGuestUser(mapOf("instance_id" to instanceId))
                Mentor.updateFromLoginResponse(response)
                if (testId.isNullOrEmpty()) {
                    navigateToNextScreen()
                } else {
                    navigateToCourseDetailsScreen(testId)
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
        if (error != null) {
            Log.i(TAG, "onInitFinished: error ${error.message}")
            analyzeAppRequirement()
            return
        }
        try {
            val jsonParams =
                referringParams ?: (Branch.getInstance().firstReferringParams
                    ?: Branch.getInstance().latestReferringParams)
            Timber.tag("BranchDeepLinkParams : ")
                .d("jsonParams = $jsonParams, error = $error")
            var testId: String? = null
            var exploreType: String? = null
            jsonParams?.let {
                AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                if (it.has(Defines.Jsonkey.AndroidDeepLinkPath.key)) {
                    testId =
                        it.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                } else if (it.has(Defines.Jsonkey.ContentType.key)) {
                    exploreType = if (it.has(Defines.Jsonkey.ContentType.key)) {
                        it.getString(Defines.Jsonkey.ContentType.key)
                    } else null
                }
                updateReferralModel(jsonParams)
                if (it.has(DeepLinkData.NOTIFICATION_ID.key) && it.has(DeepLinkData.NOTIFICATION_CHANNEL.key)) {
                    addDeepLinkNotificationAnalytics(
                        it.getString((DeepLinkData.NOTIFICATION_ID.key)),
                        it.getString(DeepLinkData.NOTIFICATION_CHANNEL.key)
                    )
                }

//                if (it.has(Defines.Jsonkey.Clicked_Branch_Link.key).not() ||
//                    it.getBoolean(Defines.Jsonkey.Clicked_Branch_Link.key).not()
//                ) {
                AppObjectController.uiHandler.postDelayed({
                    analyzeAppRequirement()
                }, 700)
//                }

            }
            if (isFinishing.not()) {
                initReferral(testId = testId, exploreType = exploreType, jsonParams)
                initAfterBranch(
                    testId = testId, exploreType = exploreType,
                    jsonParams = if (jsonParams.has(DeepLinkData.REDIRECT_TO.key)) jsonParams else null
                )
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            startNextActivity()
            LogException.catchException(ex)
        }
    }

    private fun updateReferralModel(jsonParams: JSONObject) {
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
}