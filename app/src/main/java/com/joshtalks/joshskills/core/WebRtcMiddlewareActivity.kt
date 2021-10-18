package com.joshtalks.joshskills.core

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.voip.WebRtcCallback
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.ui.voip.voip_rating.VoipCallFeedbackActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

open class WebRtcMiddlewareActivity : CoreJoshActivity() {
    private var mBoundService: WebRtcService? = null
    private var mServiceBound = false

    private var myConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.addListener(callback)
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    if (WebRtcService.isCallOnGoing.value == true) {
                        findViewById<View>(R.id.ongoing_call_container)?.setOnClickListener {
                            mBoundService?.openConnectedCallActivity(this@WebRtcMiddlewareActivity)
                        }
                        findViewById<View>(R.id.ongoing_call_container)?.visibility = View.VISIBLE
                        val callType = mBoundService?.getCallType()
                        val callConnected = mBoundService?.isCallerJoined ?: false

                        if (callType == CallType.OUTGOING) {
                            if (callConnected) {
                                callTimerUi()
                            } else {
                                findViewById<Chronometer>(R.id.call_timer).visibility = View.GONE
                            }
                        } else {
                            callTimerUi()
                        }
                    } else {
                        findViewById<View>(R.id.ongoing_call_container)?.visibility = View.GONE
                    }
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceBound = false
        }
    }

    private var callback: WebRtcCallback = object : WebRtcCallback {
        override fun onConnect(callId: String) {
            super.onConnect(callId)
            lifecycleScope.launch(Dispatchers.Main) {
                callTimerUi()
            }
        }

        override fun onDisconnect(callId: String?, channelName: String?, time: Long) {
            super.onDisconnect(callId, channelName, time)
            lifecycleScope.launchWhenResumed {
                findViewById<View>(R.id.ongoing_call_container)?.visibility = View.GONE
                findViewById<View>(R.id.ongoing_call_container)?.setOnClickListener(null)
                if (time > 0 && channelName.isNullOrEmpty().not()) {
                    VoipCallFeedbackActivity.startPtoPFeedbackActivity(
                        channelName = channelName,
                        callTime = time,
                        callerName = mBoundService?.getOppositeCallerName(),
                        callerImage = mBoundService?.getOppositeCallerProfilePic(),
                        yourName = if (User.getInstance().firstName.isNullOrBlank()) "New User" else User.getInstance().firstName,
                        yourAgoraId = mBoundService?.getUserAgoraId(),
                        dimBg = true,
                        activity = this@WebRtcMiddlewareActivity,
                        flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                    this@WebRtcMiddlewareActivity.finish()
                }
                mBoundService?.setOppositeUserInfo(null)
            }
        }
    }

    private fun callTimerUi() {
        with(findViewById<Chronometer>(R.id.call_timer)) {
            base = SystemClock.elapsedRealtime() - mBoundService?.getTimeOfTalk()!!
            start()
            visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isScreenOpenByConversationRoom) {
            bindService(Intent(this, WebRtcService::class.java), myConnection, BIND_AUTO_CREATE)
        }else{
            PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, true)
        }
    }

    override fun onStop() {
        try {
            if (!isScreenOpenByConversationRoom) {
                unbindService(myConnection)
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        super.onStop()
    }

    companion object{
        var isScreenOpenByConversationRoom = false

    }

    override fun getConversationId(): String? {
        return super.getConversationId()
    }
}
