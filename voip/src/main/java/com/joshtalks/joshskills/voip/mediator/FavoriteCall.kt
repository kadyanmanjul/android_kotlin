package com.joshtalks.joshskills.voip.mediator

import android.widget.RemoteViews
import com.joshtalks.joshskills.voip.communication.model.IncomingCall

class FavoriteCall : CallCategory {
    override fun notificationLayout(map: HashMap<String, String>): RemoteViews? {
        // TODO: Incoming Notification
        return super.notificationLayout(map)
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