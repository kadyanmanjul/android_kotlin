package com.joshtalks.joshskills.quizgame.util

import android.app.Activity
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.voip.WebRtcCallback
import com.joshtalks.joshskills.ui.voip.WebRtcService
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import java.lang.ref.WeakReference

class P2pRtc {
    companion object{
        @Volatile
        @JvmStatic
        private var mRtcEngine: RtcEngine? = null

        @Volatile
        private var callCallback: WeakReference<WebRtcEngineCallback>? = null

    }
    private var eventListener: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
            override fun onError(err: Int) {
            }

            override fun onLeaveChannel(stats: RtcStats) {
                super.onLeaveChannel(stats)
                callCallback?.get()?.onDisconnect("","")
            }
            override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            }

            override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                super.onUserJoined(uid, elapsed)
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                super.onUserOffline(uid, reason)
                callCallback?.get()?.onPartnerLeave()
            }

            override fun onActiveSpeaker(uid: Int) {
                super.onActiveSpeaker(uid)
                //showToast("Active Speaker")
            }

             override fun onConnectionLost() {
                super.onConnectionLost()
                 //showToast("Connection lost")
             }
        }

     fun initEngine(activity: Activity): RtcEngine? {
        mRtcEngine = RtcEngine.create(activity,"569a477f372a454b8101fc89ec6161e6",eventListener)
        return mRtcEngine
     }

    fun addListener(callback: WebRtcEngineCallback?) {
        callCallback = WeakReference(callback)
    }
    interface WebRtcEngineCallback {
        fun onChannelJoin() {}
        fun onConnect(callId: String) {}
        fun onDisconnect(callId: String?, channelName: String?) {}
        fun onCallReject(callId: String?) {}
        fun switchChannel(data: HashMap<String, String?>) {}
        fun onNetworkLost() {}
        fun onNetworkReconnect() {}
        fun onSpeakerOff() {}
        fun onPartnerLeave(){}
    }
}