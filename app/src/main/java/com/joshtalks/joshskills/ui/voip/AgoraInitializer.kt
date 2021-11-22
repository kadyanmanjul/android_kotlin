package com.joshtalks.joshskills.ui.voip

import android.content.Context
import android.media.AudioManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import timber.log.Timber

object AgoraInitializer {
    private const val TAG = "AgoraInitializer"

    @JvmStatic
    private val callReconnectTime = AppObjectController.getFirebaseRemoteConfig()
        .getLong(FirebaseRemoteConfigKey.VOIP_CALL_RECONNECT_TIME)
    private val audioManager: AudioManager by lazy {
        AppObjectController.joshApplication.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager
    }
    private var rtcEngine: RtcEngine? = null
    var isEngineInitialized = false

    fun initEngine(callback: (RtcEngine) -> Unit) {
        rtcEngine = AppObjectController.getRtcEngine(AppObjectController.joshApplication)
        try {
            try {
                Thread.sleep(350)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (eventListener != null) {
                rtcEngine?.removeHandler(eventListener)
            }
            if (eventListener != null) {
                rtcEngine?.addHandler(eventListener)
            }
            if (rtcEngine != null && isEngineInitialized) {
                callback.invoke(rtcEngine!!)
                return
            }

            rtcEngine?.apply {
                if (BuildConfig.DEBUG) {
                    setParameters("{\"rtc.log_filter\": 65535}")
                    setParameters("{\"che.audio.start_debug_recording\":\"all\"}")
                }
                setParameters("{\"rtc.peer.offline_period\":$callReconnectTime")
                setParameters("{\"che.audio.keep.audiosession\":true}")

                disableVideo()
                enableAudio()
                enableAudioVolumeIndication(1500, 3, true)
                setAudioProfile(
                    Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                    Constants.AUDIO_SCENARIO_EDUCATION
                )
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                adjustRecordingSignalVolume(400)
                val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                adjustPlaybackSignalVolume((95 / maxVolume) * currentVolume)
                enableDeepLearningDenoise(true)
                // Configuration for the publisher. When the network condition is poor, send audio only.
                setLocalPublishFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)

                // Configuration for the subscriber. Try to receive low stream under poor network conditions. When the current network conditions are not sufficient for video streams, receive audio stream only.
                setRemoteSubscribeFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)
            }
            if (rtcEngine != null) {
                isEngineInitialized = true
                callback.invoke(rtcEngine!!)
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            Timber.tag(TAG).e("onAudioRouteChanged=  $routing")
        }

        private fun bluetoothDisconnected() {
            audioManager?.apply {
                mode = AudioManager.MODE_IN_COMMUNICATION
                stopBluetoothSco()
                isBluetoothScoOn = false
            }
        }

        private fun bluetoothConnected() {
            audioManager?.apply {
                mode = AudioManager.MODE_IN_COMMUNICATION
                startBluetoothSco()
                isBluetoothScoOn = true
            }
        }

        override fun onError(errorCode: Int) {
            Timber.tag(TAG).e("onError=  $errorCode")
            super.onError(errorCode)
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Timber.tag(TAG).e("onJoinChannelSuccess=  $channel = $uid   ")
        }

        override fun onLeaveChannel(stats: RtcStats) {
            Timber.tag(TAG).e("onLeaveChannel=  %s", stats.totalDuration)
            super.onLeaveChannel(stats)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Timber.tag(TAG).e("onUserJoined=  $uid  $elapsed   ${rtcEngine?.connectionState}")
            super.onUserJoined(uid, elapsed)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Timber.tag(TAG).e("onUserOffline=  $uid  $reason")
            super.onUserOffline(uid, reason)
        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onRejoinChannelSuccess(channel, uid, elapsed)
            Timber.tag(TAG).e("onRejoinChannelSuccess")
            gainNetwork()
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Timber.tag(TAG).e("onConnectionLost")
//            lostNetwork()
        }

        private fun lostNetwork(time: Long) {}

        private fun gainNetwork() {}

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            super.onConnectionStateChanged(state, reason)
            Timber.tag(TAG).e("onConnectionStateChanged $state $reason")
        }

        override fun onChannelMediaRelayStateChanged(state: Int, code: Int) {
            super.onChannelMediaRelayStateChanged(state, code)
            Timber.tag(TAG).e("onChannelMediaRelayStateChanged")
        }

        override fun onMediaEngineLoadSuccess() {
            super.onMediaEngineLoadSuccess()
            Timber.tag(TAG).e("onMediaEngineLoadSuccess")
        }

        override fun onMediaEngineStartCallSuccess() {
            super.onMediaEngineStartCallSuccess()
            Timber.tag(TAG).e("onMediaEngineStartCallSuccess")
        }

        override fun onNetworkTypeChanged(type: Int) {
            super.onNetworkTypeChanged(type)
            Timber.tag(TAG).e("onNetworkTypeChanged1= $type ")
        }
    }
}