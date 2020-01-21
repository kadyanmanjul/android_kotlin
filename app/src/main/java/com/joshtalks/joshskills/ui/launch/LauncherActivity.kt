package com.joshtalks.joshskills.ui.launch

import android.os.Bundle
import android.os.Handler
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_ACTIVATED_APP
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.service.WorkMangerAdmin

class LauncherActivity : CoreJoshActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppObjectController.facebookEventLogger.logEvent(EVENT_NAME_ACTIVATED_APP)
        WorkMangerAdmin.readMessageUpdating()
        setContentView(R.layout.activity_launcher)
        val intent = getIntentForState()
        Handler().postDelayed({
            startActivity(intent)
            this@LauncherActivity.finish()

        }, 2000)

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
