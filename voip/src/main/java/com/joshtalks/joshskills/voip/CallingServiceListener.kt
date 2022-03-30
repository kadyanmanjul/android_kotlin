package com.joshtalks.joshskills.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

const val SERVICE_BROADCAST_KEY = "service_broadcast_key"
const val START_SERVICE = true
const val STOP_SERVICE = false
const val CALLING_SERVICE_ACTION = "com.joshtalks.joshskills.CALLING_SERVICE"

class CallingServiceReceiver: BroadcastReceiver(){

    override fun onReceive(p0: Context?, p1: Intent?) {
        if(p1?.action == CALLING_SERVICE_ACTION) {
            when (p1.getBooleanExtra(SERVICE_BROADCAST_KEY, false)) {
                true -> {
//                        TODO:START SERVICE
                    voipLog?.log("onReceive: start service")
                }
                false -> {
//                        TODO:STOP SERVICE
                    voipLog?.log("onReceive: stop service")
                }
            }
        }
    }
}
