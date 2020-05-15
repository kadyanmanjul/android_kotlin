package com.joshtalks.joshskills.ui.launch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogFragment
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.payment.STARTED_FROM
import io.branch.referral.Branch
import io.branch.referral.Defines
import org.json.JSONObject


class LauncherActivity : CoreJoshActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Branch.getInstance(applicationContext).resetUserSession()
        WorkMangerAdmin.appStartWorker()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        val tManager: TelephonyManager? =
            baseContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        // launcher Activity analytics
        AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.NETWORK_CARRIER.NAME, tManager?.networkOperatorName)
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
                if (error == null && jsonParms != null && jsonParms.has(Defines.Jsonkey.AndroidDeepLinkPath.key)) {
                    AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                    val testId =
                        jsonParms.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                    WorkMangerAdmin.registerUserGIDWithTestId(testId)
                    val referralCode =
                        if (jsonParms.has(Defines.Jsonkey.ReferralCode.key)) jsonParms.getString(
                            Defines.Jsonkey.ReferralCode.key
                        ) else null
                    AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId ?: EMPTY)
                        .addParam(
                            AnalyticsEvent.REFERRAL_CODE.NAME,
                            referralCode
                        )
                        .push()
                    startActivity(
                        Intent(
                            applicationContext,
                            PaymentActivity::class.java
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            putExtra(COURSE_ID, testId.split("_")[1])
                            putExtra(STARTED_FROM, this@LauncherActivity.javaClass.simpleName)

                        })
                    this@LauncherActivity.finish()
                }
            } catch (ex: Throwable) {
                LogException.catchException(ex)
            }
        }.withData(this.intent.data).init()

    }

    override fun onStart() {
        super.onStart()
        handleIntent()

        val oemIntent = PowerManagers.getIntentForOEM(this)
        val performedAction = PrefManager.getStringValue(CUSTOM_PERMISSION_ACTION_KEY)
        if (
            oemIntent != null &&
            (performedAction == EMPTY || performedAction == PermissionAction.CANCEL.name)
        ) {
            showCustomPermissionDialog(oemIntent)
        } else {
            AppObjectController.uiHandler.postDelayed({
                val intent = getIntentForState()
                startActivity(intent)
                this@LauncherActivity.finish()
            }, 2500)
        }
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

    /**
     *  Show fragment asking for custom permission to start app in background for proper working of notifications
     */
    private fun showCustomPermissionDialog(intent: Intent) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("custom_permission_fragment_dialog")
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }
        fragmentTransaction.addToBackStack(null)
        CustomPermissionDialogFragment.newInstance(intent)
            .show(supportFragmentManager, "custom_permission_fragment_dialog")
    }

}
