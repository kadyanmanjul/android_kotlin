package com.joshtalks.joshskills.ui.launch

import android.content.Intent
import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import org.json.JSONObject


class LauncherActivity : CoreJoshActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Branch.getInstance(applicationContext).resetUserSession()
        Branch.getInstance(applicationContext).initSession()
        WorkMangerAdmin.appStartWorker()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        handleIntent()
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
                ex.printStackTrace()
            }
        }.withData(this.intent.data)

        /*Branch.getInstance().initSession({ referringParams, error ->

        }, this.intent.data, this)*/
    }

    override fun onStart() {
        super.onStart()
        Branch.getInstance().initSession(BranchListener, this.intent.data, this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        Branch.getInstance().reInitSession(this, BranchListener)
        handleIntent()
    }

    private object BranchListener : Branch.BranchReferralInitListener {
        override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
            Crashlytics.log(error?.message)
        }
    }

    override fun onResume() {
        super.onResume()
        AppObjectController.uiHandler.postDelayed({
            val intent = getIntentForState()
            startActivity(intent)
            this@LauncherActivity.finish()
        }, 2000)
    }


    override fun onBackPressed() {
        super.onBackPressed()
        this.finishAndRemoveTask()
    }

    /*

  private fun fbId(){
        if (BuildConfig.DEBUG) {
            FirebaseInstanceId.getInstance().instanceId
                .addOnSuccessListener { result ->
                    Log.d("IID_TOKEN", result.token)
                }
        }

    }


        try {
            val epoch = "1579199231".toLong()
            val instant = Instant.ofEpochSecond(epoch)
            Log.e(
                "time",
                "" + ZonedDateTime.ofInstant(
                    instant,
                    ZoneOffset.UTC
                ).hour
            )
            Log.e(
                "time",
                "" + ZonedDateTime.ofInstant(
                    instant,
                    ZoneOffset.UTC
                ).toString()
            )
            Log.e(
                "time",
                "" + ZonedDateTime.ofInstant(
                    instant,
                    ZoneOffset.UTC
                ).toEpochSecond()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }


    public Instant now() {
        return Instant.now();
    }

    public ZonedDateTime hereAndNow() {
        return ZonedDateTime.ofInstant(now(), ZoneId.systemDefault());
    }

    }

*/


}
