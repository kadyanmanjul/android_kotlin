package com.joshtalks.joshskills.ui.launch

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.TelephonyManager
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CLEAR_CACHE
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.IS_GUEST_ENROLLED
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LOCALE_UPDATED
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogInteractionListener
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_NEW
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import io.branch.referral.Branch
import io.branch.referral.Defines
import kotlinx.android.synthetic.main.activity_launcher.progress_bar
import org.json.JSONObject
import timber.log.Timber
import java.io.File


class LauncherActivity : CoreJoshActivity(), CustomPermissionDialogInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        WorkManager.getInstance(applicationContext).cancelAllWork()
        animatedProgressBar()
        Branch.getInstance(applicationContext).resetUserSession()
        WorkManagerAdmin.appStartWorker()
        logAppLaunchEvent(getNetworkOperatorName())
        AppObjectController.initialiseFreshChat()
        clearGlideCache()
        logNotificationData()
    }

    private fun logNotificationData() {
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
                        .addParam(AnalyticsEvent.TOTAL_NOTIFICATION_CHANNELS.NAME, channels.size)
                        .addParam(AnalyticsEvent.CHANNELS_ENABLED.NAME, listChannelsOn)
                        .addParam(AnalyticsEvent.CHANNELS_ENABLED_SIZE.NAME, listChannelsOn.size)
                        .addParam(AnalyticsEvent.CHANNELS_DISABLED.NAME, listChannelsOff)
                        .addParam(AnalyticsEvent.CHANNELS_DISABLED_SIZE.NAME, listChannelsOff.size)
                        .push()
                }
            }
        } catch (ex: Exception) {
            LogException.catchException(ex)
        }
    }

    private fun animatedProgressBar() {
        val backgroundColorAnimator: ObjectAnimator = ObjectAnimator.ofObject(
            progress_bar, "backgroundColor", ArgbEvaluator(), -0x1, -0x873a07
        )
        backgroundColorAnimator.duration = 300
        backgroundColorAnimator.start()
    }

    private fun clearGlideCache() {
        if (PrefManager.hasKey(CLEAR_CACHE).not()) {
            Glide.get(applicationContext).clearMemory()
            JoshSkillExecutors.BOUNDED.execute {
                Glide.get(applicationContext).clearDiskCache()
            }
            PrefManager.put(CLEAR_CACHE, true)
        }
    }

    private fun handleIntent() {
        Branch.sessionBuilder(this).withCallback { referringParams, error ->
            try {
                val jsonParams = referringParams ?: (Branch.getInstance().firstReferringParams
                    ?: Branch.getInstance().latestReferringParams)
                Timber.tag("BranchDeepLinkParams : ")
                    .d("referringParams = $referringParams, error = $error")
                if (error == null && jsonParams?.has(Defines.Jsonkey.AndroidDeepLinkPath.key) == true) {
                    AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                    val testId = jsonParams.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                    WorkManagerAdmin.registerUserGAID(testId)
                    val referralCode = parseReferralCode(jsonParams)
                    referralCode?.let {
                        logInstallByReferralEvent(testId, null, it)
                    }
                    navigateToCourseDetailsScreen(testId)
                    this@LauncherActivity.finish()
                }
                if (error == null && jsonParams?.has(Defines.Jsonkey.ContentType.key) == true) {
                    val exploreType = if (jsonParams.has(Defines.Jsonkey.ContentType.key)) {
                        jsonParams.getString(Defines.Jsonkey.ContentType.key)
                    } else null
                    WorkManagerAdmin.registerUserGAID(null, exploreType)
                    val referralCode = parseReferralCode(jsonParams)
                    referralCode?.let {
                        logInstallByReferralEvent(null, exploreType, it)
                    }
                }
            } catch (ex: Throwable) {
                LogException.catchException(ex)
            }
        }.withData(this.intent.data).init()

    }

    override fun onStart() {
        super.onStart()
        handleIntent()
        navigateToNextScreen()
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

    override fun navigateToNextScreen() {
        Handler().postDelayed({
            val versionResponse = VersionResponse.getInstance()

            if (versionResponse.version == null) {
                navigateToNextScreen()
            } else {
                when (versionResponse.version!!.name) {
                    ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7 -> {
                        startNextActivity()
                    }
                    ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V5, ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                        if (PrefManager.getBoolValue(
                                IS_GUEST_ENROLLED,
                                false
                            ) || User.getInstance().isVerified
                        ) {
                            startNextActivity()
                        } else {
                            Glide.with(AppObjectController.joshApplication)
                                .downloadOnly().load(versionResponse.image)
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .listener(object : RequestListener<File> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<File>?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: File?,
                                        model: Any?,
                                        target: Target<File>?,
                                        dataSource: DataSource?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        OnBoardingActivityNew.startOnBoardingActivity(
                                            this@LauncherActivity,
                                            COURSE_EXPLORER_NEW,
                                            false
                                        )
                                        this@LauncherActivity.finish()
                                        return false
                                    }

                                }).submit()

                        }
                    }
                    ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                        if (PrefManager.getBoolValue(USER_LOCALE_UPDATED)) {
                            startNextActivity()
                        } else {
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
                }
            }
        }, 500)
    }

    private fun startNextActivity() {
        val intent = getIntentForState()
        startActivity(intent)
        this@LauncherActivity.finish()
    }

    private fun navigateToCourseDetailsScreen(testId: String) {
        CourseDetailsActivity.startCourseDetailsActivity(
            this,
            testId.split("_")[1].toInt(),
            this@LauncherActivity.javaClass.simpleName,
            flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            buySubscription = false
        )
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
}
