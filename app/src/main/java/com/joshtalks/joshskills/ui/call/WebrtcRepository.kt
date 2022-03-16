package com.joshtalks.joshskills.ui.call

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.voip.data.WebrtcService

private const val TAG = "WebrtcRepository"
class WebrtcRepository {
    private var mService : Messenger? = null
    private var handler : Messenger? = null

    private val remoteServiceIntent = Intent(AppObjectController.joshApplication, WebrtcService::class.java)

    init {
        val bundle = Bundle().apply {
            putString("token", "${PrefManager.getStringValue(API_TOKEN)}")
            putString("mentor", "${ Mentor.getInstance().getUserId()}")
        }
        remoteServiceIntent.putExtras(bundle)
        AppObjectController.joshApplication.startService(remoteServiceIntent)

    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = Messenger(service)
            handler = Messenger(IncomingHandler())
            // After Connection must send handler to service to listen for messages
            sendMessage(2)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
    }

    fun startService() {
        AppObjectController.joshApplication.bindService(remoteServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun sendMessage(data : Int) {
        /**
         * 1. JWT Token
         * 2. MentorID
         * 3.
         */
        val msg = Message().apply {
            what = data
            val bundle = Bundle().apply {
                putString("token", "${PrefManager.getStringValue(API_TOKEN)}")
                putString("mentor", "${ Mentor.getInstance().getUserId()}")
            }
            if(data == 2)
                obj = bundle
                replyTo = handler
        }
        mService?.send(msg)
    }
}

private class IncomingHandler() : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        Log.d(TAG, "handleMessage: ${msg.what}")
        when(msg.what) {
            2 -> {

            }
            else -> super.handleMessage(msg)
        }
    }
}