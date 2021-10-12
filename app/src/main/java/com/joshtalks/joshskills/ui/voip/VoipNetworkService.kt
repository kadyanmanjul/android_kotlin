package com.joshtalks.joshskills.ui.voip

import android.app.Service
import android.app.VoiceInteractor
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.joshtalks.joshskills.ui.voip.constants.MSG_REGISTER_CLIENT
import com.joshtalks.joshskills.ui.voip.constants.MSG_UNREGISTER_CLIENT
import java.lang.ref.WeakReference

class VoipNetworkService : Service() {
    private val clients = mutableListOf<Messenger>()
    private val messenger = Messenger(VoipNetworkHandler(this))

    // NOTE : Should not use any post delayed msg
    internal inner class VoipNetworkHandler(context: Context,
                             private val applicationContext: Context = context.applicationContext
    ) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_REGISTER_CLIENT -> {

                }
                MSG_UNREGISTER_CLIENT -> {

                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return messenger.binder
    }
}