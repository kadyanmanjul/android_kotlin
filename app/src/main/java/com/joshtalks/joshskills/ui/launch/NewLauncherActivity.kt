package com.joshtalks.joshskills.ui.launch

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.service.getGoogleAdId
import com.joshtalks.joshskills.databinding.ActivityNewLauncherBinding
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import io.branch.referral.Branch
import io.branch.referral.Defines
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber


class NewLauncherActivity : CoreJoshActivity() {
    private var testId: String? = null
    private val apiRun: AtomicBoolean = AtomicBoolean(false)
    private lateinit var binding: ActivityNewLauncherBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_new_launcher)
        binding.lifecycleOwner = this
        binding.handler = this
        animatedLogo()
        initApp()
        initAppInFirstTime()
        handleIntent()
        AppObjectController.uiHandler.postDelayed({
            analyzeAppRequirement()
        }, 2000)
    }

    private fun initApp() {
        WorkManager.getInstance(applicationContext).cancelAllWork()
        Branch.getInstance(applicationContext).resetUserSession()
        WorkManagerAdmin.appInitWorker()
        logAppLaunchEvent(getNetworkOperatorName())
    }

    private fun animatedLogo() {
        ObjectAnimator.ofFloat(binding.imageView, View.ALPHA, 1f, 0f).setDuration(2000).start();
        ObjectAnimator.ofFloat(binding.retry, View.ALPHA, 1f, 0f).setDuration(2000).start();
        ObjectAnimator.ofFloat(binding.textTv, View.ALPHA, 1f, 0f).setDuration(2000).start();
    }

    private fun analyzeAppRequirement() {
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

    private fun handleIntent() {
        Branch.sessionBuilder(WeakReference(this).get()).withCallback { referringParams, error ->
            try {
                val jsonParams = referringParams ?: (Branch.getInstance().firstReferringParams
                    ?: Branch.getInstance().latestReferringParams)
                Timber.tag("BranchDeepLinkParams : ")
                    .d("referringParams = $referringParams, error = $error")
                var testId: String? = null
                var exploreType: String? = null

                if (error == null) {
                    AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                    if (jsonParams?.has(Defines.Jsonkey.AndroidDeepLinkPath.key) == true) {
                        testId = jsonParams.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
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
        }.withData(this.intent.data).init()
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
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finishAndRemoveTask()
    }

    private fun startNextActivity() {
        AppObjectController.uiHandler.postDelayed({
            WorkManagerAdmin.appStartWorker()
            AppObjectController.uiHandler.removeCallbacksAndMessages(null)
            val intent = getIntentForState()
            startActivity(intent)
            this@NewLauncherActivity.finishAndRemoveTask()
        }, 2000)
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
            .push()


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
        CoroutineScope(Dispatchers.IO).launch {
            val obj = RequestRegisterGAId()
            obj.test = testId?.split("_")?.get(1)?.toInt()
            try {
                if (PrefManager.hasKey(USER_UNIQUE_ID).not()) {
                    val id = getGoogleAdId(this@NewLauncherActivity)
                    if (id.isNullOrBlank()) {
                        return@launch
                    } else {
                        PrefManager.put(USER_UNIQUE_ID, id)
                    }
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
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
                    //retry.visibility = View.VISIBLE
                }
                ex.printStackTrace()
            }
        }
    }

    private fun getMentorForUser(instanceId: String, testId: String?) {
        CoroutineScope(Dispatchers.IO).launch {
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

    private fun navigateToCourseDetailsScreen(testId: String) {
        WorkManagerAdmin.appStartWorker()
        CourseDetailsActivity.startCourseDetailsActivity(
            this,
            testId.split("_")[1].toInt(),
            this.javaClass.simpleName,
            buySubscription = false
        )
        this.finish()
    }

}
