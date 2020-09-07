package com.joshtalks.joshskills.ui.launch

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import com.google.firebase.perf.metrics.AddTrace
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.IS_GUEST_ENROLLED
import com.joshtalks.joshskills.core.ONBOARDING_VERSION_KEY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
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
import java.lang.reflect.Type


class LauncherActivity : CoreJoshActivity(), CustomPermissionDialogInteractionListener {

    @AddTrace(name = "LauncherActivity - onCreate", enabled = true)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        animatedProgressBar()
        Branch.getInstance(applicationContext).resetUserSession()
        WorkManagerAdmin.appStartWorker()
        logAppLaunchEvent(getNetworkOperatorName())
        AppObjectController.initialiseFreshChat()

    }

    private fun animatedProgressBar() {
        val backgroundColorAnimator: ObjectAnimator = ObjectAnimator.ofObject(
            progress_bar, "backgroundColor", ArgbEvaluator(), -0x1, -0x873a07
        )
        backgroundColorAnimator.duration = 300
        backgroundColorAnimator.start()
    }

    @AddTrace(name = "handleIntent", enabled = true)
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
        AppObjectController.uiHandler.postDelayed({
            val versionResponseTypeToken: Type = object : TypeToken<VersionResponse>() {}.type
            val versionResponse = AppObjectController.gsonMapper.fromJson<VersionResponse>(
                PrefManager.getStringValue(ONBOARDING_VERSION_KEY),
                versionResponseTypeToken
            )
            when (versionResponse?.version!!.name) {
                ONBOARD_VERSIONS.ONBOARDING_V1 -> {
                    val intent = getIntentForState()
                    startActivity(intent)
                }
                ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4 -> {
                    if (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false)) {
                        val intent = getIntentForState()
                        startActivity(intent)
                    } else {
                        OnBoardingActivityNew.startOnBoardingActivity(
                            this,
                            COURSE_EXPLORER_NEW,
                            false
                        )
                    }
                }
            }
            this@LauncherActivity.finish()
        }, 3500)
    }

    private fun navigateToCourseDetailsScreen(testId: String) {
        CourseDetailsActivity.startCourseDetailsActivity(
            this,
            testId.split("_")[1].toInt(),
            this@LauncherActivity.javaClass.simpleName,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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
