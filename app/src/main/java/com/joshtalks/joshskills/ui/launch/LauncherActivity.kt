package com.joshtalks.joshskills.ui.launch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogFragment.Companion.showCustomPermissionDialog
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogInteractionListener
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.payment.STARTED_FROM
import io.branch.referral.Branch
import io.branch.referral.Defines
import org.json.JSONObject


class LauncherActivity : CoreJoshActivity(), CustomPermissionDialogInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        Branch.getInstance(applicationContext).resetUserSession()
        WorkMangerAdmin.appStartWorker()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        logAppLaunchEvent(getNetworkOperatorName())
    }

    private fun handleIntent() {
        Branch.sessionBuilder(this).withCallback { referringParams, error ->
            try {
                val jsonParams = referringParams ?: (Branch.getInstance().firstReferringParams
                    ?: Branch.getInstance().latestReferringParams)
                if (error == null && jsonParams?.has(Defines.Jsonkey.AndroidDeepLinkPath.key) == true) {
                    AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                    val testId = jsonParams.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                    WorkMangerAdmin.registerUserGIDWithTestId(testId)
                    val referralCode = parseReferralCode(jsonParams)
                    referralCode?.let {
                        logInstallByReferralEvent(testId, it)
                    }
                    navigateToPaymentScreen(testId)
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
        if (oemIntent != null && shouldRequireCustomPermission()) {
            showCustomPermissionDialog(oemIntent, supportFragmentManager)
        } else {
            navigateToNextScreen()
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

    override fun navigateToNextScreen() {
        AppObjectController.uiHandler.postDelayed({
            val intent = getIntentForState()
            startActivity(intent)
            this@LauncherActivity.finish()
        }, 2500)
    }

    fun navigateToPaymentScreen(testId: String) {
        startActivity(
            Intent(
                applicationContext,
                PaymentActivity::class.java
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra(COURSE_ID, testId.split("_")[1])
                putExtra(STARTED_FROM, this@LauncherActivity.javaClass.simpleName)

            })
    }

    private fun logInstallByReferralEvent(testId: String, referralCode: String) =
        AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId)
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

    private fun shouldRequireCustomPermission(): Boolean {
        val performedAction = PrefManager.getStringValue(CUSTOM_PERMISSION_ACTION_KEY)
        return performedAction == EMPTY || performedAction == PermissionAction.CANCEL.name
    }

    private fun parseReferralCode(jsonParams: JSONObject) =
        if (jsonParams.has(Defines.Jsonkey.ReferralCode.key)) jsonParams.getString(
            Defines.Jsonkey.ReferralCode.key
        ) else null
}
