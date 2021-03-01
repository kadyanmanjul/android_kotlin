package com.joshtalks.joshskills.ui.launch

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import io.branch.referral.Branch
import io.branch.referral.Defines
import kotlinx.android.synthetic.main.activity_launcher.*
import org.json.JSONObject
import timber.log.Timber


class LauncherActivity : CoreJoshActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        initApp()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        animatedProgressBar()
        initAppInFirstTime()
        startNextActivity()
    }

    private fun initApp() {
        WorkManager.getInstance(applicationContext).cancelAllWork()
        Branch.getInstance(applicationContext).resetUserSession()
        WorkManagerAdmin.appStartWorker()
        logAppLaunchEvent(getNetworkOperatorName())
        AppObjectController.initialiseFreshChat()
        //logNotificationData()
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
    private fun animatedProgressBar() {
        val backgroundColorAnimator: ObjectAnimator = ObjectAnimator.ofObject(
            progress_bar, "backgroundColor", ArgbEvaluator(), -0x1, -0x873a07
        )
        backgroundColorAnimator.duration = 300
        backgroundColorAnimator.start()
        retry.setOnClickListener {
            handleIntent()
            retry.visibility = View.INVISIBLE
        }
    }

    private fun handleIntent() {
        JoshSkillExecutors.BOUNDED.submit {

            Branch.sessionBuilder(this).withCallback { referringParams, error ->
                try {
                    val jsonParams = referringParams ?: (Branch.getInstance().firstReferringParams
                        ?: Branch.getInstance().latestReferringParams)
                    Timber.tag("BranchDeepLinkParams : ")
                        .d("referringParams = $referringParams, error = $error")
                    var testId: String? = null
                    var exploreType: String? = null

                    if (error == null) {
                        if (jsonParams?.has(Defines.Jsonkey.AndroidDeepLinkPath.key) == true) {
                            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                            testId = jsonParams.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                        } else if (jsonParams?.has(Defines.Jsonkey.ContentType.key) == true) {
                            exploreType = if (jsonParams.has(Defines.Jsonkey.ContentType.key)) {
                                jsonParams.getString(Defines.Jsonkey.ContentType.key)
                            } else null
                        }
                    }
                    initReferral(testId = testId, exploreType = exploreType, jsonParams)
                    initAfterBranch(testId = testId, exploreType = exploreType)
                } catch (ex: Throwable) {
                    startNextActivity()
                    LogException.catchException(ex)
                }
            }.withData(this.intent.data).init()
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

    override fun onStart() {
        super.onStart()
        handleIntent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent()
    }

    override fun onStop() {
        super.onStop()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME).endSession()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finishAndRemoveTask()
    }

    private fun startNextActivity() {
        val intent = getIntentForState()
        startActivity(intent)
        this@LauncherActivity.finish()
    }

    private fun logInstallByReferralEvent(
        testId: String?,
        exploreType: String?,
        referralCode: String
    ) = AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME)
        .addBasicParam()
        .addUserDetails()
        .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId)
        .addParam(AnalyticsEvent.EXPLORE_TYPE.NAME, exploreType)
        .addParam(
            AnalyticsEvent.REFERRAL_CODE.NAME,
            referralCode
        )
        .push()

    private fun logAppLaunchEvent(networkOperatorName: String) =
        AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.NETWORK_CARRIER.NAME, networkOperatorName)
            .push(true)


    private fun getNetworkOperatorName() =
        (baseContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperatorName
            ?: ""

    private fun parseReferralCode(jsonParams: JSONObject) =
        if (jsonParams.has(Defines.Jsonkey.ReferralCode.key)) jsonParams.getString(
            Defines.Jsonkey.ReferralCode.key
        ) else null

    private fun initAppInFirstTime() {
        if (Utils.isInternetAvailable().not() && PrefManager.hasKey(SERVER_GID_ID)) {
            startNextActivity()
        }
    }

    private fun initAfterBranch(testId: String? = null, exploreType: String? = null) {
        when {
            testId != null -> {
                initGaid(testId, exploreType)
            }
            PrefManager.hasKey(SERVER_GID_ID) -> {
                startNextActivity()
            }
            else -> {
                initGaid(testId)
            }
        }
    }

    private fun initGaid(testId: String? = null, exploreType: String? = null) {
        val uuid = WorkManagerAdmin.initGaid(testId, exploreType)
        val observer = Observer<WorkInfo> { workInfo ->
            workInfo?.run {
                if (WorkInfo.State.SUCCEEDED == state) {
                    if (testId.isNullOrEmpty()) {
                        navigateToNextScreen()
                    } else {
                        navigateToCourseDetailsScreen(testId)
                    }
                } else if (WorkInfo.State.FAILED == state) {
                    retry.visibility = View.VISIBLE
                }
            }
        }
        WorkManager.getInstance(applicationContext)
            .getWorkInfoByIdLiveData(uuid)
            .observe(this, observer)
    }

    private fun navigateToCourseDetailsScreen(testId: String) {
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
/*
        JoshSkillExecutors.BOUNDED.submit {
            val versionResponse = VersionResponse.getInstance()
            if (null == versionResponse.version) {
                startNextActivity()
            } else {
                val isGuestEnrolled = PrefManager.getBoolValue(IS_GUEST_ENROLLED)
                val isUserVerified = User.getInstance().isVerified
                val userLocale = PrefManager.getBoolValue(USER_LOCALE_UPDATED)
                when (versionResponse.version!!.name) {
                    ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7 -> {
                        startNextActivity()
                    }
                    ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V5, ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                        if (isGuestEnrolled || isUserVerified) {
                            startNextActivity()
                        } else {
                            startOnboardingNewActivity()
                        }
                    }
                    ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                        if (userLocale) {
                            startNextActivity()
                        } else {
                            changeLanguageOfApp()
                        }
                    }
                }
            }
        }*/
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
