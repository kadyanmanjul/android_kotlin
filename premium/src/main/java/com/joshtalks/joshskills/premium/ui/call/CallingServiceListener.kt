package com.joshtalks.joshskills.premium.ui.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.joshtalks.joshskills.premium.BuildConfig
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_STOP_SERVICE
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.premium.core.API_TOKEN
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.USER_LOCALE
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.premium.calling.Utils
import com.joshtalks.joshskills.premium.calling.data.CallingRemoteService

private const val TAG = "CallingServiceReceiver"
class CallingServiceReceiver : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == CALLING_SERVICE_ACTION) {
            when (p1.getBooleanExtra(SERVICE_BROADCAST_KEY, false)) {
                true -> {
                    Log.d(TAG, "onReceive: start service")
                    val remoteServiceIntent =
                        Intent(
                            AppObjectController.joshApplication,
                            CallingRemoteService::class.java
                        )
                    val apiHeader = ApiHeader(
                        token = "JWT " + PrefManager.getStringValue(API_TOKEN),
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE.toString(),
                        userAgent = "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString(),
                        acceptLanguage = PrefManager.getStringValue(USER_LOCALE)
                    )
                    remoteServiceIntent.putExtra(
                        INTENT_DATA_MENTOR_ID,
                        Mentor.getInstance().getId()
                    )
                    remoteServiceIntent.putExtra(INTENT_DATA_API_HEADER, apiHeader)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        AppObjectController.joshApplication.startForegroundService(remoteServiceIntent)
                    } else {
                        AppObjectController.joshApplication.startService(remoteServiceIntent)
                    }
                }
                false -> {
                    Log.d(TAG, "onReceive: stop service")
                    val remoteServiceIntent =
                        Intent(
                            AppObjectController.joshApplication,
                            CallingRemoteService::class.java
                        )
                    remoteServiceIntent.action = SERVICE_ACTION_STOP_SERVICE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        AppObjectController.joshApplication.startForegroundService(remoteServiceIntent)
                    } else {
                        AppObjectController.joshApplication.startService(remoteServiceIntent)
                    }
                }
            }
        }
    }
}
