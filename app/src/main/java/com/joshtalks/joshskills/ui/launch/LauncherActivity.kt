package com.joshtalks.joshskills.ui.launch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import io.branch.referral.Branch
import io.branch.referral.Defines
import io.sentry.core.Sentry
import org.json.JSONObject


class LauncherActivity : CoreJoshActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Branch.getInstance(applicationContext).resetUserSession()
        WorkMangerAdmin.appStartWorker()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        val tManager: TelephonyManager = baseContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        // launcher Activity analytics
        AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME)
            .addParam(AnalyticsEvent.APP_VERSION_CODE.NAME, BuildConfig.VERSION_NAME)
            .addParam(AnalyticsEvent.NETWORK_CARRIER.NAME, tManager.networkOperatorName)
            .addParam(AnalyticsEvent.SOURCE.NAME, InstallReferrerModel.getPrefObject()?.utmSource ?: EMPTY)
            .addParam(AnalyticsEvent.USER_GAID.NAME,PrefManager.getStringValue(USER_UNIQUE_ID))
            .addParam(AnalyticsEvent.SOURCE.NAME, InstallReferrerModel.getPrefObject()?.utmSource ?: EMPTY)
            .push(true)

    }

    private fun handleIntent() {
        Branch.sessionBuilder(this).withCallback { referringParams, error ->
            try {
                var jsonParms: JSONObject? = referringParams
                val sessionParams = Branch.getInstance().latestReferringParams
                val installParams = Branch.getInstance().firstReferringParams
                if (referringParams == null && installParams != null) {
                    jsonParms = installParams
                } else if (referringParams == null && sessionParams != null) {
                    jsonParms = sessionParams
                }
                if (error == null) {
                    if (jsonParms != null && jsonParms.has(Defines.Jsonkey.AndroidDeepLinkPath.key)) {
                        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                        val testId =
                            jsonParms.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                        WorkMangerAdmin.registerUserGIDWithTestId(testId)
                        AppAnalytics.create(AnalyticsEvent.APP_INSTALL_WITH_DEEP_LINK.NAME)
                            .addParam(AnalyticsEvent.APP_VERSION_CODE.NAME, BuildConfig.VERSION_NAME)
                            .addParam(AnalyticsEvent.DEVICE_MANUFACTURER.NAME,Build.MANUFACTURER)
                            .addParam(AnalyticsEvent.DEVICE_MODEL.NAME,Build.MODEL)
                            .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId?:EMPTY)
                            .addParam(AnalyticsEvent.USER_GAID.NAME, PrefManager.getStringValue(USER_UNIQUE_ID))
                            .addParam(AnalyticsEvent.USER_NAME.NAME, User.getInstance()?.firstName ?:EMPTY)
                            .addParam(AnalyticsEvent.USER_EMAIL.NAME, User.getInstance()?.email ?: EMPTY)
                            .addParam(AnalyticsEvent.SOURCE.NAME, InstallReferrerModel.getPrefObject()?.utmSource ?: EMPTY)

                            .push()
                        startActivity(
                            Intent(
                                applicationContext,
                                PaymentActivity::class.java
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                putExtra(COURSE_ID, testId.split("_")[1])
                            })
                        this@LauncherActivity.finish()
                    }
                }
            } catch (ex: Exception) {
                Sentry.captureException(ex)
                ex.printStackTrace()
            }
        }.withData(this.intent.data).init()

    }

    override fun onStart() {
        super.onStart()
        handleIntent()
        AppObjectController.uiHandler.postDelayed({
            val intent = getIntentForState()
            startActivity(intent)
            this@LauncherActivity.finish()
        }, 2500)
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
}