package com.joshtalks.joshskills.ui.launch

import android.content.Intent
import android.os.Bundle
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
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
                            .addParam("test_id", testId).push()
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

    override fun onBackPressed() {
        super.onBackPressed()
        this.finishAndRemoveTask()
    }
}