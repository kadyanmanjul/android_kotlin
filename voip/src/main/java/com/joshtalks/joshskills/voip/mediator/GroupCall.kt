package com.joshtalks.joshskills.voip.mediator

import android.widget.RemoteViews
import com.joshtalks.joshskills.voip.communication.model.IncomingCall

class GroupCall : CallCategory {
    override fun notificationLayout(data: IncomingCall): RemoteViews? {
        // TODO: Incoming Notification
        return super.notificationLayout(data)
    }

    override suspend fun onPreCallConnect(
        callData: HashMap<String, Any>,
        direction: CallDirection
    ) {
        super.onPreCallConnect(callData, direction)
    }

    override suspend fun onCallDecline(callData: HashMap<String, Any>) {
        super.onCallDecline(callData)
    }
}