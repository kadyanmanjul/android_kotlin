package com.joshtalks.joshskills.ui.launch

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.START_SERVICE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.notification.HAS_LOCAL_NOTIFICATION
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.ActivityLauncherBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.call.CallingServiceReceiver
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.util.DeepLinkData
import com.joshtalks.joshskills.util.DeepLinkImpression
import com.joshtalks.joshskills.util.DeepLinkRedirect
import com.joshtalks.joshskills.util.DeepLinkRedirectUtil
import com.yariksoffice.lingver.Lingver
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class LauncherActivity : CoreJoshActivity(), Branch.BranchReferralInitListener {
    var APP_PACKAGE_COUNT = 2
    private var testId: String? = null
    private val viewModel: LauncherViewModel by lazy {
        ViewModelProvider(this).get(LauncherViewModel::class.java)
    }
    private var jsonParams: JSONObject? = null

    private val binding by lazy {
        ActivityLauncherBinding.inflate(layoutInflater)
    }

    companion object {
        @JvmStatic
        var isLingverInit = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        AppObjectController.init()
        AppObjectController.initFirebaseRemoteConfig()
        AppObjectController.configureCrashlytics()
        viewModel.initApp()
        AppObjectController.initFonts()
        initiateLibraries()
        WorkManagerAdmin.runMemoryManagementWorker()
        LogSaver.startSavingLog() // to save logs in external storage
        animatedProgressBar()
        handleIntent()
        setObservers()
        AppObjectController.getNewArchVoipFlag()
        AppObjectController.initObjectInThread()
        VoipPref.initVoipPref(this)
        AppObjectController.initMoEngage()
        AppObjectController.registerBroadcastReceiver()
    }

    @Synchronized
    private fun initiateLibraries() {
        if (!isLingverInit) {
            isLingverInit = true
            if (PrefManager.getStringValue(USER_LOCALE).isEmpty()) {
                PrefManager.put(USER_LOCALE, "en")
            }
            Lingver.init(application, PrefManager.getStringValue(USER_LOCALE))
        }
    }

    private fun setObservers() {
        viewModel.apiCallStatus.observe(this) {
            when (it) {
                ApiCallStatus.START -> binding.progressBar.visibility = View.VISIBLE
                ApiCallStatus.SUCCESS -> {
                    binding.progressBar.visibility = View.GONE
                    startNextActivity()
                }
                ApiCallStatus.FAILED -> {
                    binding.retry.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
                else -> binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun animatedProgressBar() {
        val backgroundColorAnimator: ObjectAnimator = ObjectAnimator.ofObject(
            binding.progressBar, "backgroundColor", ArgbEvaluator(), -0x1, -0x873a07
        )
        backgroundColorAnimator.duration = 300
        backgroundColorAnimator.start()
        binding.retry.setOnClickListener {
            if (Utils.isInternetAvailable().not()) {
                Snackbar.make(binding.root, getString(R.string.internet_not_available_msz), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.settings)) {
                        startActivity(
                            Intent(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                    Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                                else
                                    Settings.ACTION_WIRELESS_SETTINGS
                            )
                        )
                    }.show()
            } else {
                analyzeAppRequirement()
                binding.retry.visibility = View.INVISIBLE
            }
        }
    }

    private fun analyzeAppRequirement() {
        when {
            PrefManager.getStringValue(USER_UNIQUE_ID).isEmpty() -> {
                if (intent.data == null)
                    viewModel.initGaid(testId)
            }
            Mentor.getInstance().hasId() -> startNextActivity()
            else -> viewModel.getMentorForUser(
                PrefManager.getStringValue(USER_UNIQUE_ID),
                testId
            )
        }
    }

    private fun handleIntent() {
        if (intent.hasExtra(HAS_LOCAL_NOTIFICATION) &&
            intent.getBooleanExtra(HAS_LOCAL_NOTIFICATION, false)
        ) {
            PrefManager.put(
                LOCAL_NOTIFICATION_INDEX,
                PrefManager.getIntValue(LOCAL_NOTIFICATION_INDEX, defValue = 0).plus(1)
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        if (intent.hasExtra(Defines.IntentKeys.ForceNewBranchSession.key) &&
            intent.getBooleanExtra(Defines.IntentKeys.ForceNewBranchSession.key, false)
        ) {
            Branch.sessionBuilder(this).withCallback(this@LauncherActivity).reInit()
        }
        handleIntent()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this@LauncherActivity)
            .registerReceiver(CallingServiceReceiver(), IntentFilter(CALLING_SERVICE_ACTION))
        val isCourseBought = PrefManager.getBoolValue(IS_COURSE_BOUGHT, false)
        val courseExpiryTime =
            PrefManager.getLongValue(COURSE_EXPIRY_TIME_IN_MS)
        if ((isCourseBought && User.getInstance().isVerified) || courseExpiryTime != 0L &&
            courseExpiryTime >= System.currentTimeMillis()
        ) {
            val broadcastIntent = Intent().apply {
                action = CALLING_SERVICE_ACTION
                putExtra(SERVICE_BROADCAST_KEY, START_SERVICE)
            }
            LocalBroadcastManager.getInstance(this@LauncherActivity).sendBroadcast(broadcastIntent)
        }
        if (Utils.isInternetAvailable()) {
            Branch.sessionBuilder(this)
                .withCallback(this@LauncherActivity)
                .withData(this.intent?.data)
                .init()
        } else {
            lifecycleScope.launch {
                delay(700)
                analyzeAppRequirement()
            }
        }
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
        this@LauncherActivity.finishAndRemoveTask()
    }

    private fun startNextActivity() {
        if(canRunApplication()) {
            WorkManagerAdmin.appStartWorker()
            lifecycleScope.launch {
                viewModel.addAnalytics()
                viewModel.updateABTestCampaigns()
                AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                if (testId.isNullOrEmpty().not()) {
                    navigateToCourseDetailsScreen()
                } else {
                    getIntentForNextActivity()?.let {
                        startActivity(it)
                    }
                    finish()
                }
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("Alert!!!")
                .setMessage("App will not run on VM Environment")
                .setPositiveButton("OK"
                ) { p0, p1 -> finishAndRemoveTask() }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()
        }
    }

    suspend fun getIntentForNextActivity() =
        when {
            User.getInstance().isVerified.not() -> {
                when {
                    (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false) &&
                            PrefManager.getBoolValue(IS_PAYMENT_DONE, false).not()) -> {
                        if (jsonParams != null &&
                            (DeepLinkRedirectUtil.getIntent(
                                this@LauncherActivity,
                                jsonParams!!,
                                true
                            ))
                        ) {
                            null
                        } else getInboxActivityIntent()
                    }
                    PrefManager.hasKey(
                        SPECIFIC_ONBOARDING,
                        isConsistent = true
                    ) || (jsonParams != null && jsonParams!!.has(DeepLinkData.REDIRECT_TO.key)
                            && jsonParams!!.getString(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.ONBOARDING.key)
                    -> DeepLinkRedirectUtil.getIntentForCourseOnboarding(this, jsonParams)
                    (jsonParams != null && jsonParams!!.has(DeepLinkData.REDIRECT_TO.key)
                            && jsonParams!!.getString(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.COURSE_DETAILS.key)
                    -> {
                        DeepLinkRedirectUtil.getCourseDetailsActivityIntent(this, jsonParams!!)
                        null
                    }
                    PrefManager.getBoolValue(IS_PAYMENT_DONE, false) ->
                        Intent(this@LauncherActivity, SignUpActivity::class.java)
                    PrefManager.getBoolValue(IS_FREE_TRIAL, false, false) ->
                        Intent(
                            this@LauncherActivity,
                            FreeTrialOnBoardActivity::class.java
                        )
                    else ->
                        Intent(this@LauncherActivity, SignUpActivity::class.java)
                }
            }
            isUserProfileNotComplete() -> Intent(this@LauncherActivity, SignUpActivity::class.java)
            jsonParams != null -> if (DeepLinkRedirectUtil.getIntent(
                    this@LauncherActivity,
                    jsonParams!!,
                    PrefManager.getBoolValue(IS_FREE_TRIAL)
                )
            ) null else getInboxActivityIntent()
            else -> getInboxActivityIntent()
        }

    private fun initAfterBranch(
        exploreType: String? = null
    ) {
        when {
            testId != null -> viewModel.initGaid(testId, exploreType)
            PrefManager.hasKey(SERVER_GID_ID) ->
                if (PrefManager.hasKey(API_TOKEN)) {
                    startNextActivity()
                } else {
                    viewModel.getMentorForUser(PrefManager.getStringValue(USER_UNIQUE_ID), testId)
                }
            Mentor.getInstance().hasId() -> startNextActivity()
            else -> viewModel.initGaid(testId)
        }
    }

    override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
        if (error != null) {
            analyzeAppRequirement()
            return
        }
        try {
            var exploreType: String? = null
            (referringParams ?: (Branch.getInstance().firstReferringParams
                ?: Branch.getInstance().latestReferringParams))?.let { jsonParams ->
                Log.d("LauncherActivity.kt", "YASH => onInitFinished: jsonParams: $jsonParams")
                AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                if (jsonParams.has(Defines.Jsonkey.AndroidDeepLinkPath.key)) {
                    testId =
                        jsonParams.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                } else if (jsonParams.has(Defines.Jsonkey.ContentType.key)) {
                    exploreType = if (jsonParams.has(Defines.Jsonkey.ContentType.key)) {
                        jsonParams.getString(Defines.Jsonkey.ContentType.key)
                    } else null
                }
                if (jsonParams.has(DeepLinkData.NOTIFICATION_ID.key) &&
                    jsonParams.has(DeepLinkData.NOTIFICATION_CHANNEL.key)
                ) {
                    viewModel.addDeepLinkNotificationAnalytics(
                        jsonParams.getString((DeepLinkData.NOTIFICATION_ID.key)),
                        jsonParams.getString(DeepLinkData.NOTIFICATION_CHANNEL.key)
                    )
                }
                if (jsonParams.has(Defines.Jsonkey.UTMMedium.key) &&
                    jsonParams.getString(Defines.Jsonkey.UTMMedium.key) == "referral"
                ) {
                    viewModel.saveDeepLinkImpression(
                        deepLink = (
                                if (jsonParams.has(DeepLinkData.REFERRING_LINK.key))
                                    jsonParams.getString(DeepLinkData.REFERRING_LINK.key)
                                else ""
                                ),
                        action = DeepLinkImpression.REFERRAL.name
                    )
                    viewModel.updateReferralModel(jsonParams)
                    viewModel.initReferral(testId = testId, exploreType = exploreType, jsonParams)
                }
                if (jsonParams.has(DeepLinkData.REDIRECT_TO.key)) {
                    this.jsonParams = jsonParams
                    if(jsonParams.getString(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.LOGIN.key){
                        PrefManager.put(LOGIN_ONBOARDING, true)
                    }
                    viewModel.saveDeepLinkImpression(
                        deepLink = (
                                if (jsonParams.has(DeepLinkData.REFERRING_LINK.key))
                                    jsonParams.getString(DeepLinkData.REFERRING_LINK.key)
                                else ""
                                ),
                        action = "${DeepLinkImpression.REDIRECT_}${
                            jsonParams.getString(DeepLinkData.REDIRECT_TO.key).uppercase()
                        }${
                            if (jsonParams.getString(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.ONBOARDING.key)
                                "_${jsonParams.getString(DeepLinkData.COURSE_ID.key)}"
                            else if (jsonParams.getString(DeepLinkData.REDIRECT_TO.key) == DeepLinkRedirect.COURSE_DETAILS.key)
                                "_${jsonParams.getString(DeepLinkData.TEST_ID.key)}"
                            else ""
                        }"
                    )
                }
                initAfterBranch(exploreType = exploreType)
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            startNextActivity()
            LogException.catchException(ex)
        }
    }

    private fun navigateToCourseDetailsScreen() {
        try {
            CourseDetailsActivity.startCourseDetailsActivity(
                this,
                testId!!.split("_")[1].toInt(),
                this@LauncherActivity.javaClass.simpleName,
                buySubscription = false
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        this@LauncherActivity.finish()
    }

    private fun canRunApplication() : Boolean {
        val path = this.filesDir.absolutePath
        val count = getDotCount(path)
        return count <= APP_PACKAGE_COUNT
    }

    private fun getDotCount(path: String): Int {
        var count = 0
        for (i in 0 until path.length) {
            if (count > APP_PACKAGE_COUNT) {
                break
            }
            if (path[i] == '.') {
                count++
            }
        }
        return count
    }

    private fun showDialog() {

    }
}