package com.joshtalks.joshskills.quizgame.calling

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.text.TextUtils
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.printAll
import com.joshtalks.joshskills.quizgame.ui.main.view.fragment.FavouritePartnerFragment
import com.joshtalks.joshskills.ui.voip.*
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber
import java.lang.Exception
import java.lang.ref.WeakReference

class WebRtcEngine :BaseWebRtcService(){
    private var isEngineInitialized = false
    private var isMicEnabled = true

    companion object{
        private val TAG = WebRtcEngine::class.java.simpleName
        private var mRtcEngine: RtcEngine? = null
        private var callCallback: WeakReference<WebRtcEngineCallback>? = null

        fun initLibrary() {
            val serviceIntent = Intent(AppObjectController.joshApplication, WebRtcEngine::class.java)
                .apply{
                action = InitLibrary().action
            }
            AppObjectController.joshApplication.startService(serviceIntent)
        }
    }
    private fun initEngine(callback: () -> Unit) {
        try {
            mRtcEngine = AppObjectController.getRtcEngine(AppObjectController.joshApplication)
            try {
                Thread.sleep(350)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (eventListener != null) {
                mRtcEngine?.removeHandler(eventListener)
            }
            if (eventListener != null) {
                mRtcEngine?.addHandler(eventListener)
            }
            if (isEngineInitialized) {
                callback.invoke()
                return
            }
           mRtcEngine?.apply {
                disableVideo()
                enableAudio()
                enableAudioVolumeIndication(500, 3, true)
                setAudioProfile(
                    Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                    Constants.AUDIO_SCENARIO_EDUCATION
                )
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                adjustRecordingSignalVolume(400)
                val audio = getSystemService(Service.AUDIO_SERVICE) as AudioManager
                val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val currentVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                adjustPlaybackSignalVolume((95 / maxVolume) * currentVolume)
                enableDeepLearningDenoise(true)
                // Configuration for the publisher. When the network condition is poor, send audio only.
                setLocalPublishFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)

                // Configuration for the subscriber. Try to receive low stream under poor network conditions. When the current network conditions are not sufficient for video streams, receive audio stream only.
                setRemoteSubscribeFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)
            }
            if (mRtcEngine != null) {
                isEngineInitialized = true
                callback.invoke()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        isEngineInitialized = false
    }

    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {
        private val micMutex = Mutex()
        private val speakerMutex = Mutex()


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
            Timber.tag(TAG).e("onUserJoined=  $uid  $elapsed" + "   " + mRtcEngine?.connectionState)
            super.onUserJoined(uid, elapsed)

        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Timber.tag(TAG).e("onUserOffline=  $uid  $reason")
            super.onUserOffline(uid, reason)
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
            Timber.tag(TAG).e("onNetworkTypeChanged1=     " + type)
        }
    }

     fun joinCall(data: String) {
         var accessToken: String? = "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
         if (TextUtils.equals(accessToken, "") || TextUtils.equals(
                 accessToken,
                 "006569a477f372a454b8101fc89ec6161e6IADYmZ3FSiuZkvYIVERzwHjKvcLGDMAC5LxQ8lh7CxFfjhQWp0e379yDIgAkhwQAweFmYQQAAQBB4YpjAgBB4YpjAwBB4YpjBABB4Ypj"
             )
         ) {
             accessToken = null
         }
         val statusCode = mRtcEngine?.joinChannel(accessToken, data, "Extra Optional Data", 0)

         //val statusCode: Int = mRtcEngine?.joinChannel(accessToken, data, "Extra Optional Data", 0, option)

         if (statusCode != null) {
             if (statusCode < 0) {
                 initEngine {
                     joinCall(data)
                 }
             }
         }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        executor.execute {
            intent?.action.run {
                initEngine {
                    try {
                        val data : String ? =intent?.getStringExtra(CALL_USER_OBJ)
//                        val data: HashMap<String, String?> =
//                            intent?.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                        joinCall(data!!)
                    }catch (ex:Exception){
                        Timber.d(ex.message)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun getToken(data: HashMap<String, String?>): String? {
        return data[RTC_TOKEN_KEY]
    }

    private fun getChannelName(data: HashMap<String, String?>): String? {
        return data[RTC_CHANNEL_KEY]
    }

    private fun getUID(data: HashMap<String, String?>): Int {
        return data[RTC_UID_KEY]?.toInt() ?: 0
    }

    fun switchSpeck() {
        executor.submit {
            try {
                if (isMicEnabled) {
                    unMuteCall()
                    isMicEnabled = !isMicEnabled
                } else {
                    muteCall()
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun muteCall() {
        mRtcEngine?.muteLocalAudioStream(true)
    }

    fun unMuteCall() {
        mRtcEngine?.muteLocalAudioStream(false)
    }

    fun addListener(callback: WebRtcEngineCallback?) {
        callCallback = WeakReference(callback)
    }


    inner class MyBinder:Binder(){
        fun getService() : WebRtcEngine{
            return this@WebRtcEngine
        }
    }
}
sealed class WebEngineCalling
data class InitLibrary(val action: String = "calling.action.initLibrary") : WebEngineCalling()

interface WebRtcEngineCallback {
    fun onChannelJoin() {}
    fun onConnect(callId: String) {}
    fun onDisconnect(callId: String?, channelName: String?) {}
    fun onCallReject(callId: String?) {}
    fun switchChannel(data: HashMap<String, String?>) {}
    fun onNetworkLost() {}
    fun onNetworkReconnect() {}
    fun onSpeakerOff() {}
}

