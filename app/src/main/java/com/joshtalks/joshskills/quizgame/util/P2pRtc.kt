package com.joshtalks.joshskills.quizgame.util

import android.app.Activity
import android.app.Service
import android.content.Context
import android.media.AudioManager
import androidx.core.content.ContextCompat.getSystemService
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.voip.WebRtcCallback
import com.joshtalks.joshskills.ui.voip.WebRtcService
import io.agora.rtc.Constants
import io.agora.rtc.Constants.AUDIO_ROUTE_HEADSET
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import timber.log.Timber
import java.lang.ref.WeakReference

class P2pRtc {
    companion object {
        @Volatile
        @JvmStatic
        var mRtcEngine: RtcEngine? = null
        private var isSpeakerEnabled = false
        private var isMicEnabled = true

        private var activityContext: Activity? = null
        var callCallback: WebRtcEngineCallback? = null
    }

    private var eventListener: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onError(err: Int) {
        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
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
            callCallback?.onPartnerLeave()
        }

        override fun onActiveSpeaker(uid: Int) {
            super.onActiveSpeaker(uid)
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
        }

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            if (routing == AUDIO_ROUTE_HEADSET) {
                bluetoothDisconnected()
                callCallback?.onSpeakerOff()
                isSpeakerEnabled = false
                mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(isSpeakerEnabled)
            } else if (routing == Constants.AUDIO_ROUTE_HEADSETBLUETOOTH) {
                callCallback?.onSpeakerOff()
                isSpeakerEnabled = false
                bluetoothConnected()
            } else {
                bluetoothDisconnected()
            }
        }
    }

    private fun bluetoothDisconnected() {
        val audioManager: AudioManager =
            activityContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
    }

    private fun bluetoothConnected() {
        val audioManager: AudioManager =
            activityContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
    }

    fun switchAudioSpeaker() {
        val audioManager = activityContext?.getSystemService(Service.AUDIO_SERVICE) as AudioManager
        isSpeakerEnabled = !isSpeakerEnabled
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        mRtcEngine?.setEnableSpeakerphone(isSpeakerEnabled)
        audioManager.isSpeakerphoneOn = isSpeakerEnabled
    }

    fun getSpeaker() = isSpeakerEnabled
    fun getMic() = isMicEnabled

    fun initEngine(activity: Activity): RtcEngine? {
        activityContext = activity
        mRtcEngine = RtcEngine.create(activity, "569a477f372a454b8101fc89ec6161e6", eventListener)
        return mRtcEngine
    }

    fun getEngineObj(): RtcEngine? {
        return mRtcEngine
    }

    fun addListener(callback: WebRtcEngineCallback?) {
        callCallback = callback
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
        fun onPartnerLeave() {}
    }
}