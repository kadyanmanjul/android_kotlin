/*
package com.joshtalks.badebhaiya.ui.launcher

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.HAS_SEEN_LOCAL_NOTIFICATION
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.core.LAST_FAKE_CALL_INVOKE_TIME
import com.joshtalks.joshskills.core.LOCAL_NOTIFICATION_INDEX
import com.joshtalks.joshskills.core.PREF_IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SERVER_GID_ID
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.notification.HAS_LOCAL_NOTIFICATION
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import io.branch.referral.Branch
import io.branch.referral.Defines
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.android.synthetic.main.activity_launcher.progress_bar
import kotlinx.android.synthetic.main.activity_launcher.retry
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import timber.log.Timber

private const val TAG = "LauncherActivity"

class LauncherActivity : AppCompatActivity(), LifecycleObserver {
    private var testId: String? = null
    private val apiRun: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        initApp()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        animatedProgressBar()
        initAppInFirstTime()
        handleIntent()
        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
        AppObjectController.uiHandler.postDelayed({
            analyzeAppRequirement()
        }, 700)
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
            Branch.getInstance(applicationContext).resetUserSession()
            logAppLaunchEvent(getNetworkOperatorName())
            if (PrefManager.hasKey(IS_FREE_TRIAL).not() && User.getInstance().isVerified.not()) {
                PrefManager.put(IS_FREE_TRIAL, true, false)
            }
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
            Log.i(TAG, "analyzeAppRequirement: ")
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
        lifecycleScope.launch(Dispatchers.IO) {
            Branch.sessionBuilder(WeakReference(this@LauncherActivity).get())
                .withCallback { referringParams, error ->
                    try {
                        val jsonParams =
                            referringParams ?: (Branch.getInstance().firstReferringParams
                                ?: Branch.getInstance().latestReferringParams)
                        Timber.tag("BranchDeepLinkParams : ")
                            .d("jsonParams = $jsonParams, error = $error")
                        var testId: String? = null
                        var exploreType: String? = null
                        val installReferrerModel =
                            InstallReferrerModel.getPrefObject() ?: InstallReferrerModel()
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
                            if (it.has(Defines.Jsonkey.ReferralCode.key))
                                installReferrerModel.utmSource =
                                    it.getString(Defines.Jsonkey.ReferralCode.key)
                            if (it.has(Defines.Jsonkey.UTMMedium.key))
                                installReferrerModel.utmMedium =
                                    it.getString(Defines.Jsonkey.UTMMedium.key)
                            if (it.has(Defines.Jsonkey.UTMCampaign.key))
                                installReferrerModel.utmTerm =
                                    it.getString(Defines.Jsonkey.UTMCampaign.key)
                            InstallReferrerModel.update(installReferrerModel)
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
            */
/*Firebase.dynamicLinks
                .getDynamicLink(intent)
                .addOnSuccessListener { pendingDynamicLinkData ->
                    var deepLink: Uri? = null
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.link
                    }
                    if (deepLink != null) {
                        Snackbar.make(findViewById(android.R.id.content),
                            "Found deep link!$deepLink", Snackbar.LENGTH_LONG).show()
                        Log.e(TAG, "initApp: ${pendingDynamicLinkData.toString()}")
                        Log.e(TAG, "initApp: deepLink=> ${pendingDynamicLinkData.link}")
                        Log.e(TAG, "initApp: utmParameters=> ${pendingDynamicLinkData.utmParameters}")
                        Log.e(TAG, "initApp: clickTimestamp=> ${pendingDynamicLinkData.clickTimestamp}")
                        Log.e(TAG, "initApp: extensions=> ${pendingDynamicLinkData.extensions}")
                    } else {
                        Log.d(TAG, "getDynamicLink: no link found")
                    }
                }
                .addOnFailureListener { e -> Log.w(TAG, *//*
*/
/*"getDynamicLink:onFailure", e) }*//*

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
//        intent.putExtra("branch_force_new_session", true)
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

    private fun navigateToNextScreen() {
        startNextActivity()
    }
}
*/
