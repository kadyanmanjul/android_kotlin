package com.joshtalks.joshskills.core.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.core.interfaces.UsbEventListener

class UsbEventReceiver : BroadcastReceiver() {
    var listener: UsbEventListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals("android.hardware.usb.action.USB_STATE", ignoreCase = true)) {
            if (intent.extras!!.getBoolean("connected")) {
                // USB was connected
                notifyListener(true)
            } else {
                // USB was disconnected
                notifyListener(false)
            }
        }
    }

    fun notifyListener(connected: Boolean) {
        listener?.let {
            if (connected) it.onUsbConnect()
            else it.onUsbDisconnect()
        }
    }
}