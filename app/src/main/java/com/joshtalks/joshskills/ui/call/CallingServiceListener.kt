package com.joshtalks.joshskills.ui.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_STOP_SERVICE
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LOCALE
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.voipLog

const val SERVICE_BROADCAST_KEY = "service_broadcast_key"
const val START_SERVICE = true
const val STOP_SERVICE = false
const val CALLING_SERVICE_ACTION = "com.joshtalks.joshskills.CALLING_SERVICE"

class CallingServiceReceiver: BroadcastReceiver(){

    override fun onReceive(p0: Context?, p1: Intent?) {
        if(p1?.action == CALLING_SERVICE_ACTION) {
            when (p1.getBooleanExtra(SERVICE_BROADCAST_KEY, false)) {
                true -> {
                    voipLog?.log("onReceive: start service")
                    val remoteServiceIntent =
                        Intent(AppObjectController.joshApplication, CallingRemoteService::class.java)
                    val apiHeader = ApiHeader(
                        token = "JWT " + PrefManager.getStringValue(API_TOKEN),
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE.toString(),
                        userAgent = "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString(),
                        acceptLanguage = PrefManager.getStringValue(USER_LOCALE)
                    )
                    remoteServiceIntent.putExtra(INTENT_DATA_MENTOR_ID, Mentor.getInstance().getId())
                    remoteServiceIntent.putExtra(INTENT_DATA_API_HEADER, apiHeader)
                    AppObjectController.joshApplication.startService(remoteServiceIntent)                }
                false -> {
                    voipLog?.log("onReceive: stop service")
                    val remoteServiceIntent =
                        Intent(AppObjectController.joshApplication, CallingRemoteService::class.java)
                remoteServiceIntent.action = SERVICE_ACTION_STOP_SERVICE
                    AppObjectController.joshApplication.startService(remoteServiceIntent)                }
            }
        }
    }
}
