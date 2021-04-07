package com.joshtalks.joshskills.ui.launch

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.service.getGoogleAdId
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import io.branch.referral.Branch
import io.branch.referral.Defines
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.android.synthetic.main.activity_launcher.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber


class LauncherActivity : CoreJoshActivity() {

    init {
        Timber.d("asd123  LauncherActivity.init")
    }

    private var testId: String? = null
    private val apiRun: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("asd123  LauncherActivity.onCreate")
        initApp()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        animatedProgressBar()
        initAppInFirstTime()
        handleIntent()
        AppObjectController.uiHandler.postDelayed({
            analyzeAppRequirement()
        }, 700)
    }

    private fun initApp() {
        lifecycleScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(applicationContext).cancelAllWork()
            WorkManagerAdmin.appInitWorker()
            Branch.getInstance(applicationContext).resetUserSession()
            logAppLaunchEvent(getNetworkOperatorName())
            //logNotificationData()
        }
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
        lifecycleScope.launch(Dispatchers.IO) {
            Branch.sessionBuilder(WeakReference(this@LauncherActivity).get())
                .withCallback { referringParams, error ->
                    try {
                        val jsonParams =
                            referringParams ?: (Branch.getInstance().firstReferringParams
                                ?: Branch.getInstance().latestReferringParams)
                        Timber.tag("BranchDeepLinkParams : ")
                            .d("referringParams = $referringParams, error = $error")
                        var testId: String? = null
                        var exploreType: String? = null

                        if (error == null) {
                            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                            if (jsonParams?.has(Defines.Jsonkey.AndroidDeepLinkPath.key) == true) {
                                testId =
                                    jsonParams.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                            } else if (jsonParams?.has(Defines.Jsonkey.ContentType.key) == true) {
                                exploreType = if (jsonParams.has(Defines.Jsonkey.ContentType.key)) {
                                    jsonParams.getString(Defines.Jsonkey.ContentType.key)
                                } else null
                            }
                        }
                        if (isFinishing.not()) {
                            initReferral(testId = testId, exploreType = exploreType, jsonParams)
                            initAfterBranch(testId = testId, exploreType = exploreType)
                        }
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                        startNextActivity()
                        LogException.catchException(ex)
                    }
                }.withData(this@LauncherActivity.intent.data).init()
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
        handleIntent()
    }

    override fun onStop() {
        super.onStop()

        Branch.sessionBuilder(null)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME).endSession()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finishAndRemoveTask()
    }

    private fun startNextActivity() {
        lifecycleScope.launch(Dispatchers.IO) {
            WorkManagerAdmin.appStartWorker()
            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
            val intent = getIntentForState()
            startActivity(intent)
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
                .push(true)
        }
    }


    private fun getNetworkOperatorName() =
        (baseContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperatorName
            ?: ""

    private fun parseReferralCode(jsonParams: JSONObject) =
        if (jsonParams.has(Defines.Jsonkey.ReferralCode.key)) jsonParams.getString(
            Defines.Jsonkey.ReferralCode.key
        ) else null

    private fun initAppInFirstTime() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (Utils.isInternetAvailable().not() && PrefManager.hasKey(SERVER_GID_ID)) {
                startNextActivity()
            }
        }
    }

    private fun initAfterBranch(testId: String? = null, exploreType: String? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            when {
                testId != null -> {
                    initGaid(testId, exploreType)
                }
                PrefManager.hasKey(SERVER_GID_ID) -> {
                    if (PrefManager.hasKey(API_TOKEN)) {
                        startNextActivity()
                    } else {
                        getMentorForUser(PrefManager.getStringValue(INSTANCE_ID), testId)
                    }
                }
                Mentor.getInstance().hasId() -> {
                    startNextActivity()
                }
                else -> {
                    initGaid(testId)
                }
            }
        }
    }

    private fun navigateToCourseDetailsScreen(testId: String) {
        WorkManagerAdmin.appStartWorker()
        CourseDetailsActivity.startCourseDetailsActivity(
            this,
            testId.split("_")[1].toInt(),
            this@LauncherActivity.javaClass.simpleName,
            buySubscription = false
        )
        this@LauncherActivity.finish()
    }

    private fun navigateToNextScreen() {
        startNextActivity()
    }


    private fun initGaid(testId: String? = null, exploreType: String? = null) {
        if (apiRun.get()) {
            return
        }
        apiRun.set(true)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        this.testId = testId
        lifecycleScope.launch(Dispatchers.IO) {
            val obj = RequestRegisterGAId()
            obj.test = testId?.split("_")?.get(1)?.toInt()
            if (PrefManager.hasKey(USER_UNIQUE_ID).not()) {
                val id = getGoogleAdId(this@LauncherActivity)
                PrefManager.put(USER_UNIQUE_ID, id)
            }
            obj.gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
            InstallReferrerModel.getPrefObject()?.let {
                obj.installOn = it.installOn
                obj.utmMedium = it.utmMedium
                obj.utmSource = it.utmSource
            }

            if (exploreType.isNullOrEmpty().not()) {
                obj.exploreCardType = ExploreCardType.valueOf(exploreType!!)
            }
            try {
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
            val response =
                AppObjectController.signUpNetworkService.createGuestUser(mapOf("instance_id" to instanceId))
            Mentor.updateFromLoginResponse(response)
            if (testId.isNullOrEmpty()) {
                navigateToNextScreen()
            } else {
                navigateToCourseDetailsScreen(testId)
            }
        }
    }


    private fun startOnboardingNewActivity() {
        OnBoardingActivityNew.startOnBoardingActivity(
            this@LauncherActivity,
            COURSE_EXPLORER_NEW,
            false
        )
        this@LauncherActivity.finish()
    }

    private fun changeLanguageOfApp() {
        requestWorkerForChangeLanguage(
            "hi",
            canCreateActivity = false,
            successCallback = {
                AppObjectController.isSettingUpdate = true
                startNextActivity()
            }, errorCallback = {
                startNextActivity()
            })
    }
}


/*private fun logNotificationData() {
    lifecycleScope.launchWhenStarted {
        try {
            val isNotificationEnabled =
                NotificationManagerCompat.from(AppObjectController.joshApplication)
                    .areNotificationsEnabled()
            AppAnalytics.create(AnalyticsEvent.ARE_NOTIFICATIONS_ENABLED.NAME)
                .addUserDetails()
                .addBasicParam()
                .addParam("is_enabled", isNotificationEnabled)
                .push()

            if (isNotificationEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val a = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channels = a.notificationChannels
                if (channels.isNullOrEmpty().not() && channels.size > 0) {
                    val listChannelsOff = ArrayList<String?>()
                    val listChannelsOn = ArrayList<String?>()
                    for (channel in channels) {
                        if ((channel.importance == NotificationManager.IMPORTANCE_NONE)) {
                            listChannelsOff.add(channel.name.toString())
                        } else {
                            listChannelsOn.add(channel.name.toString())
                        }
                    }
                    AppAnalytics.create(AnalyticsEvent.ARE_NOTIFICATION_CHANNEL_ENABLED.NAME)
                        .addUserDetails()
                        .addBasicParam()
                        .addParam(
                            AnalyticsEvent.TOTAL_NOTIFICATION_CHANNELS.NAME,
                            channels.size
                        )
                        .addParam(AnalyticsEvent.CHANNELS_ENABLED.NAME, listChannelsOn)
                        .addParam(
                            AnalyticsEvent.CHANNELS_ENABLED_SIZE.NAME,
                            listChannelsOn.size
                        )
                        .addParam(AnalyticsEvent.CHANNELS_DISABLED.NAME, listChannelsOff)
                        .addParam(
                            AnalyticsEvent.CHANNELS_DISABLED_SIZE.NAME,
                            listChannelsOff.size
                        )
                        .push()
                }
            }
        } catch (ex: Exception) {
            LogException.catchException(ex)
        }
    }
}
*/
