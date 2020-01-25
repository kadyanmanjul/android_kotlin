package com.joshtalks.joshskills.ui.launch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_ACTIVATED_APP
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import io.branch.referral.validators.IntegrationValidator
import org.json.JSONObject


class LauncherActivity : CoreJoshActivity() {

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppObjectController.facebookEventLogger.logEvent(EVENT_NAME_ACTIVATED_APP)
        WorkMangerAdmin.readMessageUpdating()
        setContentView(R.layout.activity_launcher)
        handleIntent()
    }


    private fun handleIntent() {
        Branch.getInstance().initSession(object : Branch.BranchReferralInitListener {
            override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
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
                            uiHandler.removeCallbacksAndMessages(null)

                            val testId =
                                jsonParms.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
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
                    } else {
                        Log.e("BRANCH SDK", error.message)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }, this.intent.data, this)
    }

    override fun onStart() {
        super.onStart()
        Branch.getInstance().initSession(branchListener, this.intent.data, this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        Branch.getInstance().reInitSession(this, branchListener)
        handleIntent()
    }

    private object branchListener : Branch.BranchReferralInitListener {
        override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
        }
    }

    override fun onResume() {
        uiHandler.postDelayed({
            val intent = getIntentForState()
            startActivity(intent)
            this@LauncherActivity.finish()
        }, 1500)
        super.onResume()
    }
    /*


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
