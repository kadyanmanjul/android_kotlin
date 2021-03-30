package com.joshtalks.joshskills.core

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.voip.WebRtcCallback
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.voip_rating.VoipCallFeedbackView

open class WebRtcMiddlewareActivity : CoreJoshActivity() {
    private var mBoundService: WebRtcService? = null
    private var mServiceBound = false

    private var myConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.addListener(callback)
            AppObjectController.uiHandler.postDelayed(
                {
                    if (WebRtcService.isCallWasOnGoing) {
                        findViewById<View>(R.id.ongoing_call_container).visibility = View.VISIBLE
                        with(findViewById<Chronometer>(R.id.call_timer)) {
                            base = SystemClock.elapsedRealtime() - mBoundService?.getTimeOfTalk()!!
                            start()
                        }

                        findViewById<View>(R.id.ongoing_call_container).setOnClickListener {
                            mBoundService?.openConnectedCallActivity()
                        }
                    } else {
                        findViewById<View>(R.id.ongoing_call_container).visibility = View.GONE
                    }
                },
                100
            )
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceBound = false
        }
    }

    private var callback: WebRtcCallback = object : WebRtcCallback {

        override fun onDisconnect(callId: String?, channelName: String?, time: Long) {
            super.onDisconnect(callId, channelName, time)
            AppObjectController.uiHandler.postDelayed(
                {
                    findViewById<View>(R.id.ongoing_call_container).visibility = View.GONE
                    findViewById<View>(R.id.ongoing_call_container).setOnClickListener(null)

                    if (time > 0 && channelName.isNullOrEmpty().not()) {
                        VoipCallFeedbackView.showCallRatingDialog(
                            supportFragmentManager,
                            channelName = channelName,
                            callTime = time,
                            callerName = mBoundService?.getOppositeCallerName(),
                            callerImage = mBoundService?.getOppositeCallerProfilePic(),
                            yourName = if (User.getInstance().firstName.isNullOrBlank()) "New User" else User.getInstance().firstName,
                            yourAgoraId = mBoundService?.getUserAgoraId(),
                            dimBg = true
                        )
                    }
                },
                100
            )
        }
    }

    override fun onStart() {
        super.onStart()
    //    bindService(Intent(this, WebRtcService::class.java), myConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
       // unbindService(myConnection)
    }
}
