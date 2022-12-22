package com.joshtalks.joshskills.ui.launch

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Settings
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
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.notification.HAS_LOCAL_NOTIFICATION
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.ActivityLauncherBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.call.CallingServiceReceiver
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.signup.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.util.RedirectAction.*
import com.yariksoffice.lingver.Lingver
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import kotlinx.coroutines.launch
import org.json.JSONObject

const val APP_PACKAGE_COUNT = 2

class LauncherActivity : ThemedCoreJoshActivity(), Branch.BranchReferralInitListener {
    private val viewModel by lazy {
        ViewModelProvider(this)[LauncherViewModel::class.java]
    }
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
                ApiCallStatus.SUCCESS ->
                    binding.progressBar.visibility = View.GONE
                ApiCallStatus.FAILED -> {
                    binding.retry.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
                else -> binding.progressBar.visibility = View.GONE
            }
        }
        viewModel.event.observe(this) {
            when (it.what) {
                FETCH_GAID -> viewModel.getGaid()
                UPDATE_GAID -> viewModel.updateGaid()
                FETCH_MENTOR -> viewModel.getGuestMentor()
                ANALYZE_APP_REQUIREMENT -> analyzeAppRequirement()
                START_ACTIVITY -> startNextActivity()
                else -> {}
            }
        }
        viewModel.redirectEvent.observe(this) {
            val intent = when (it) {
                SIGN_UP -> Intent(this, SignUpActivity::class.java)
                INBOX -> getInboxActivityIntent()
                COURSE_ONBOARDING -> FreeTrialOnBoardActivity.getIntent(this)
                else -> null
            }
            if (PrefManager.getIntValue(LAUNCHER_SCREEN_VISIT_COUNT) < 5) {
                PrefManager.put(
                    LAUNCHER_SCREEN_VISIT_COUNT,
                    PrefManager.getIntValue(LAUNCHER_SCREEN_VISIT_COUNT).plus(1)
                )
                WorkManagerAdmin.logNextActivity(intent?.component?.className)
            }
            if (intent != null) {
                startActivity(intent)
                finish()
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
            if (Utils.isInternetAvailable()) {
                viewModel.event.postValue(Message().apply { what = ANALYZE_APP_REQUIREMENT })
                binding.retry.visibility = View.INVISIBLE
            } else {
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
            }
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
            viewModel.event.postValue(Message().apply { what = ANALYZE_APP_REQUIREMENT })
        }
    }

    override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
        if (error != null) {
            viewModel.event.postValue(Message().apply { what = ANALYZE_APP_REQUIREMENT })
            return
        }
        viewModel.jsonParams = referringParams ?: (Branch.getInstance().firstReferringParams
            ?: Branch.getInstance().latestReferringParams)
        viewModel.event.postValue(Message().apply { what = ANALYZE_APP_REQUIREMENT })
    }

    private fun analyzeAppRequirement() {
        viewModel.event.postValue(Message().apply {
            what = when {
                PrefManager.getStringValue(USER_UNIQUE_ID).isEmpty() -> FETCH_GAID
                PrefManager.getIntValue(SERVER_GID_ID, defValue = -1) == -1 -> UPDATE_GAID
                Mentor.getInstance().hasId().not() -> FETCH_MENTOR
                else -> START_ACTIVITY
            }
        })
    }

    private fun startNextActivity() {
        if (viewModel.canRunApplication()) {
            WorkManagerAdmin.appStartWorker()
            viewModel.addAnalytics()
            lifecycleScope.launch {
                viewModel.updateABTestCampaigns()
                viewModel.initDeepLinkData()
                AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                viewModel.redirectToActivity(this@LauncherActivity, isUserProfileNotComplete())
                finish()
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("Alert!!!")
                .setMessage("App will not run on VM Environment")
                .setPositiveButton(
                    "OK"
                ) { _, _ -> finishAndRemoveTask() }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()
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
}