package com.joshtalks.joshskills.ui.voip

import android.annotation.SuppressLint
import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.*
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.ACTION_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CALL_NOTIFICATION_CHANNEL
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.extra.FullScreenActivity
import com.joshtalks.joshskills.ui.voip.util.NotificationUtil
import com.joshtalks.joshskills.ui.voip.util.TelephonyUtil
import io.agora.rtc.Constants
import io.agora.rtc.Constants.CONNECTION_CHANGED_INTERRUPTED
import io.agora.rtc.Constants.CONNECTION_STATE_RECONNECTING
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


const val RTC_TOKEN_KEY = "token"
const val RTC_CHANNEL_KEY = "channel_name"
const val RTC_UID_KEY = "uid"
const val RTC_CALLER_UID_KEY = "caller_uid"
const val RTC_NAME = "caller_name"
const val RTC_CALLER_PHOTO = "caller_photo"
const val RTC_IS_FAVORITE = "is_favorite"
const val RTC_PARTNER_ID = "partner_id"


class WebRtcService : BaseWebRtcService() {

    private val mBinder: IBinder = MyBinder()
    private val hangUpRtcOnDeviceCallAnswered: PhoneStateListener =
        HangUpRtcOnPstnCallAnsweredListener()

    private var callStartTime: Long = 0
    private var callForceDisconnect = false
    private var mHandler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var userAgoraId: Int? = null
    var channelName: String? = null
    private var isEngineInit = false
    private var isCallerJoin: Boolean = false
    private var isMicEnable = true

    companion object {
        private val TAG = WebRtcService::class.java.simpleName
        var phoneCallState = CallState.CALL_STATE_IDLE

        @JvmStatic
        private val callReconnectTime = AppObjectController.getFirebaseRemoteConfig()
            .getLong(FirebaseRemoteConfigKey.VOIP_CALL_RECONNECT_TIME)

        @JvmStatic
        private val callDisconnectTime = AppObjectController.getFirebaseRemoteConfig()
            .getLong(FirebaseRemoteConfigKey.VOIP_CALL_DISCONNECT_TIME)

        @Volatile
        private var isSpeakerEnable = false


        @Volatile
        @JvmStatic
        private var mRtcEngine: RtcEngine? = null

        @Volatile
        private var callData: HashMap<String, String?>? = null

        @Volatile
        private var callId: String? = null

        @Volatile
        private var callType: CallType = CallType.OUTGOING

        @Volatile
        var isCallWasOnGoing: Boolean = false


        @Volatile
        var holdCall: Boolean = false

        @Volatile
        var retryInitLibrary: Int = 0

        @JvmStatic
        @Volatile
        var isCallRecordOngoing: Boolean = false

        @JvmStatic
        @Volatile
        var switchChannel: Boolean = false

        @Volatile
        var isTimeOutToPickCall: Boolean = false

        @Volatile
        private var callCallback: WeakReference<WebRtcCallback>? = null

        fun initLibrary() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = InitLibrary().action
            }
            AppObjectController.joshApplication.startService(serviceIntent)
        }

        fun startOutgoingCall(map: HashMap<String, String?>) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = OutgoingCall().action
                putExtra(CALL_USER_OBJ, map)
                putExtra(CALL_TYPE, CallType.OUTGOING)
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun startOnNotificationIncomingCall(data: HashMap<String, String?>) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = IncomingCall().action
                putExtra(CALL_USER_OBJ, data)
                putExtra(CALL_TYPE, CallType.INCOMING)
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun disconnectCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallDisconnect().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun disconnectCallFromCallie() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallStop().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun rejectCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallReject().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun forceConnect(data: HashMap<String, String>) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallForceConnect().action
                putExtra(CALL_USER_OBJ, data)
                putExtra(CALL_TYPE, CallType.INCOMING)
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun forceDisconnect() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallForceDisconnect().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun noUserFoundCallDisconnect() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = NoUserFound().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun holdCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = HoldCall().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun resumeCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = ResumeCall().action
            }
            serviceIntent.startServiceForWebrtc()
        }

    }

    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            Timber.tag(TAG).e("onAudioRouteChanged=  $routing")
        }

        override fun onError(errorCode: Int) {
            Timber.tag(TAG).e("onError=  $errorCode")
            super.onError(errorCode)
            if (switchChannel) {
                switchChannel = false
                return
            }
            if (isCallWasOnGoing) {
                return
            }
            RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
            disconnectService()
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Timber.tag(TAG).e("onJoinChannelSuccess=  $channel = $uid   ")
            compositeDisposable.clear()
            userAgoraId = uid
            isCallWasOnGoing = true
            callData?.let {
                channelName = getChannelName(it)
                try {
                    val id = getUID(it)
                    if ((callType == CallType.INCOMING || callType == CallType.FAVORITE_INCOMING) && id == uid) {
                        startCallTimer()
                        callStatusNetworkApi(it, CallAction.ACCEPT)
                        addNotification(CallConnect().action, callData)
                        addSensor()
                        joshAudioManager?.startCommunication()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            callId = mRtcEngine?.callId
            switchChannel = false
            if (CallType.OUTGOING == callType) {
                callCallback?.get()?.onChannelJoin()
            }
        }

        override fun onLeaveChannel(stats: RtcStats) {
            Timber.tag(TAG).e("onLeaveChannel=  %s", stats.totalDuration)
            super.onLeaveChannel(stats)
            isCallWasOnGoing = false
            if (switchChannel.not()) {
                callCallback?.get()?.onDisconnect(
                    callId,
                    callData?.let { getChannelName(it) },
                    if (isCallerJoin) {
                        TimeUnit.SECONDS.toMillis(stats.totalDuration.toLong())
                    } else {
                        getTimeOfTalk()
                    }
                )
                switchChannel = false
            }

            callData = null
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Timber.tag(TAG).e("onUserJoined=  $uid  $elapsed" + "   " + mRtcEngine?.connectionState)
            super.onUserJoined(uid, elapsed)
            compositeDisposable.clear()
            isCallWasOnGoing = true
            isCallerJoin = true
            if (callStartTime == 0L) {
                startCallTimer()
            }
            callCallback?.get()?.onConnect(uid.toString())
            mHandler?.postDelayed({
                callCallback?.get()?.onServerConnect()
            }, 500)

            addNotification(CallConnect().action, callData)
            addSensor()
            joshAudioManager?.startCommunication()
            joshAudioManager?.stopConnectTone()
            audioFocus()

        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Timber.tag(TAG).e("onUserOffline=  $uid  $reason")
            super.onUserOffline(uid, reason)
            callData?.let {
                val id = getUID(it)
                Timber.tag(TAG).e("onUserOffline =  $id")
                if (id != uid && reason == Constants.USER_OFFLINE_QUIT) {
                    endCall(apiCall = true, action = CallAction.AUTO_DISCONNECT)
                    isCallWasOnGoing = false
                } else if (id != uid && reason == Constants.USER_OFFLINE_DROPPED) {
                    lostNetwork()
                }
            }
        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onRejoinChannelSuccess(channel, uid, elapsed)
            Timber.tag(TAG).e("onRejoinChannelSuccess")
            gainNetwork()
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Timber.tag(TAG).e("onConnectionLost")
            lostNetwork()
        }

        private fun lostNetwork(time: Long = callDisconnectTime) {
            callCallback?.get()?.onNetworkLost()
            if (phoneCallState == CallState.CALL_STATE_IDLE) {
                addTimerReconnect(time)
                joshAudioManager?.startConnectTone()
            }
        }

        private fun gainNetwork() {
            compositeDisposable.clear()
            callCallback?.get()?.onNetworkReconnect()
            joshAudioManager?.stopConnectTone()
            audioFocus()
        }

        private fun audioFocus() {
            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val af = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    })
                    setAcceptsDelayedFocusGain(true)
                    build()
                }
                af.acceptsDelayedFocusGain()
                audioManager.requestAudioFocus(af)
            } else {
                audioManager.requestAudioFocus(
                    { },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            super.onConnectionStateChanged(state, reason)
            Timber.tag(TAG).e("onConnectionStateChanged    $state     $reason")
            if (CONNECTION_STATE_RECONNECTING == state && reason == CONNECTION_CHANGED_INTERRUPTED) {
                compositeDisposable.add(
                    Completable.complete()
                        .delay(5, TimeUnit.SECONDS)
                        .doOnComplete {
                            Timber.tag("Reconnect").e("doOnComplete  $isCallerJoin")
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            if (isCallerJoin) {
                                lostNetwork()
                            }
                        })
            } else {
                compositeDisposable.clear()
                gainNetwork()
            }
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

    inner class CustomHandlerThread(name: String) : HandlerThread(name) {
        override fun onLooperPrepared() {
            super.onLooperPrepared()
            mHandler = Handler(looper) { msg ->
                when (msg.what) {
                    CallState.UNHOLD.state -> {
                        phoneCallState = CallState.CALL_STATE_IDLE
                        holdCall = false
                        callCallback?.get()?.onUnHoldCall()
                        callData?.let {
                            callStatusNetworkApi(it, CallAction.RESUME)
                        }
                    }
                    CallState.ONHOLD.state -> {
                        phoneCallState = CallState.CALL_STATE_CONNECTED
                        holdCall = true
                        callCallback?.get()?.onHoldCall()
                        callData?.let {
                            callStatusNetworkApi(it, CallAction.ONHOLD)
                        }
                    }
                    CallState.EXIT.state -> {
                        RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                    }
                    CallState.CALL_HOLD_BY_OPPOSITE.state -> {
                        callCallback?.get()?.onHoldCall()
                    }
                    CallState.CALL_RESUME_BY_OPPOSITE.state -> {
                        joshAudioManager?.stopConnectTone()
                        compositeDisposable.clear()
                        callCallback?.get()?.onUnHoldCall()
                    }


                }
                true
            }
        }
    }

    inner class HangUpRtcOnPstnCallAnsweredListener : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            Timber.tag(TAG).e("RTC=    %s", state)
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    phoneCallState = CallState.CALL_STATE_IDLE
                    holdCall = false

                    val message = Message()
                    message.what = CallState.UNHOLD.state
                    mHandler?.sendMessageDelayed(message, 500)
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    val message = Message()
                    message.what = CallState.ONHOLD.state
                    mHandler?.sendMessage(message)
                }
                else -> {
                    phoneCallState = CallState.CALL_STATE_BUSY
                }
            }
        }
    }

    inner class MyBinder : Binder() {
        fun getService(): WebRtcService {
            return this@WebRtcService
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).e("onCreate")
        phoneCallState = CallState.CALL_STATE_IDLE
        handlerThread = CustomHandlerThread("WebrtcThread")
        handlerThread?.start()
        if (handlerThread != null) {
            mHandler = Handler(handlerThread!!.looper)
        }
        TelephonyUtil.getManager(this)
            .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_CALL_STATE)
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
            if (isEngineInit) {
                callback.invoke()
                return
            }
            mRtcEngine?.apply {
                if (BuildConfig.DEBUG) {
                    //     setParameters("{\"rtc.log_filter\": 65535}")
                }
                setParameters("{\"rtc.peer.offline_period\":$callReconnectTime}")
                disableVideo()
                enableAudio()
                enableAudioVolumeIndication(1000, 3, true)
                setAudioProfile(
                    Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                    Constants.AUDIO_SCENARIO_EDUCATION
                )
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                adjustRecordingSignalVolume(400)
                adjustPlaybackSignalVolume(100)
                enableInEarMonitoring(true)
                setInEarMonitoringVolume(75)

                // Configuration for the publisher. When the network condition is poor, send audio only.
                setLocalPublishFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)

                // Configuration for the subscriber. Try to receive low stream under poor network conditions. When the current network conditions are not sufficient for video streams, receive audio stream only.
                setRemoteSubscribeFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)

            }
            if (mRtcEngine != null) {
                isEngineInit = true
                callback.invoke()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand=  %s", intent?.action)
        executor.execute {
            intent?.action?.run {
                initEngine {
                    try {
                        callForceDisconnect = false
                        when {
                            this == InitLibrary().action -> {
                                Timber.tag(TAG).e("LibraryInit")
                            }
                            this == IncomingCall().action -> {
                                if (CallState.CALL_STATE_BUSY == phoneCallState || isCallWasOnGoing) {
                                    return@initEngine
                                }
                                val data =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                data.let {
                                    callData = it
                                }
                                callType = CallType.INCOMING
                                isTimeOutToPickCall = false
                                callStartTime = 0L
                                handleIncomingCall()
                            }
                            this == OutgoingCall().action -> {
                                callStartTime = 0L
                                isTimeOutToPickCall = false
                                val data: HashMap<String, String?> =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                data.let {
                                    callData = it
                                }
                                callType = CallType.OUTGOING
                                joinCall(data)
                            }
                            this == CallConnect().action -> {
                                val callData: HashMap<String, String?>? =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
                                callConnectService(callData)
                            }
                            this == CallReject().action -> {
                                addNotification(CallDisconnect().action, null)
                                callData?.let {
                                    callStatusNetworkApi(it, CallAction.DECLINE)
                                    rejectCall()
                                }
                                disconnectService()
                            }
                            this == CallDisconnect().action -> {
                                addNotification(CallDisconnect().action, null)
                                callData?.let {
                                    callStatusNetworkApi(
                                        it,
                                        CallAction.DISCONNECT,
                                        hasDisconnected = true
                                    )
                                }
                                endCall()
                                isCallRecordOngoing = false
                            }
                            this == NoUserFound().action -> {
                                callData?.let {
                                    callStatusNetworkApi(it, CallAction.NOUSERFOUND)
                                }
                                mRtcEngine?.leaveChannel()
                                callCallback?.get()?.onNoUserFound()
                                disconnectService()
                            }
                            this == CallStop().action -> {
                                addNotification(CallDisconnect().action, null)
                                callStopWithoutIssue()
                            }
                            this == CallForceDisconnect().action -> {
                                callForceDisconnect = true
                                if (JoshApplication.isAppVisible.not()) {
                                    addNotification(CallDisconnect().action, null)
                                }
                                endCall(apiCall = false)
                                RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                            }
                            this == CallForceConnect().action -> {
                                callStartTime = 0L
                                compositeDisposable.clear()
                                switchChannel = true
                                if (isCallWasOnGoing) {
                                    mRtcEngine?.leaveChannel()
                                }
                                resetConfig()
                                addNotification(CallForceConnect().action, null)
                                callData?.let {
                                    callStatusNetworkApi(it, CallAction.DECLINE)
                                }
                                AppObjectController.uiHandler.postDelayed({
                                    val data =
                                        intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                    data.let {
                                        callData = it
                                    }
                                    if (callCallback != null && callCallback?.get() != null) {
                                        callCallback?.get()?.switchChannel(data)
                                    } else {
                                        startAutoPickCallActivity(false)
                                    }
                                }, 750)
                            }
                            this == HoldCall().action -> {
                                val message = Message()
                                message.what = CallState.CALL_HOLD_BY_OPPOSITE.state
                                mHandler?.sendMessage(message)
                            }
                            this == ResumeCall().action -> {
                                compositeDisposable.clear()
                                val message = Message()
                                message.what = CallState.CALL_RESUME_BY_OPPOSITE.state
                                mHandler?.sendMessage(message)
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    fun addListener(callback: WebRtcCallback?) {
        callCallback = WeakReference(callback)
    }


    private fun callStopWithoutIssue() {
        callCallback?.get()?.onDisconnect(callId, callData?.let { getChannelName(it) })
        val message = Message()
        message.what = CallState.EXIT.state
        mHandler?.sendMessageDelayed(message, 1000)
        disconnectService()
    }

    private fun callConnectService(data: HashMap<String, String?>?) {
        data?.let {
            callData = it
        }
        mNotificationManager?.cancel(ACTION_NOTIFICATION_ID)
        mNotificationManager?.cancel(INCOMING_CALL_NOTIFICATION_ID)
        startAutoPickCallActivity(true)
    }

    private fun startAutoPickCallActivity(autoPick: Boolean) {
        val callActivityIntent =
            Intent(
                AppObjectController.joshApplication,
                WebRtcActivity::class.java
            ).apply {
                putExtra(CALL_TYPE, CallType.INCOMING)
                putExtra(AUTO_PICKUP_CALL, autoPick)
                putExtra(CALL_USER_OBJ, callData)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(callActivityIntent)
    }

    private fun handleIncomingCall() {
        executeEvent(AnalyticsEvent.INIT_CALL.NAME)
        addNotification(IncomingCall().action, callData)
        addTimeObservable()
    }

    private fun addTimeObservable() {
        compositeDisposable.add(
            Completable.complete()
                .delay(10, TimeUnit.SECONDS)
                .doOnComplete {
                    if (isCallNotConnected()) {
                        isTimeOutToPickCall = true
                        disconnectCallFromCallie()
                    }
                }
                .subscribe())
    }

    private fun addTimerReconnect(callDisconnectTime: Long) {
        compositeDisposable.add(
            Completable.complete()
                .delay(callDisconnectTime, TimeUnit.MILLISECONDS)
                .doOnComplete {
                    Timber.tag("WebRtcService").e("doOnComplete")
                }
                .subscribeOn(Schedulers.io())
                .subscribe {
                    Timber.tag("WebRtcService").e("Complete")
                    endCall(apiCall = true)
                })

    }

    fun isCallNotConnected(): Boolean {
        return (mRtcEngine?.connectionState == Constants.CONNECTION_STATE_DISCONNECTED || isCallWasOnGoing.not())
    }

    fun timeoutCaller() {
        callData?.let {
            callStatusNetworkApi(it, CallAction.TIMEOUT)
        }
    }

    private fun rejectCall() {
        try {
            callCallback?.get()?.onCallReject(callId)
        } catch (ex: Throwable) {
            RxBus2.publish(WebrtcEventBus(CallState.REJECT))
            ex.printStackTrace()
        }
        disconnectService()
    }

    fun endCall(apiCall: Boolean = false, action: CallAction = CallAction.DISCONNECT) {
        Timber.tag(TAG).e("call_status%s", mRtcEngine?.connectionState)
        if (apiCall) {
            callData?.let {
                callStatusNetworkApi(it, action)
            }
        }
        joshAudioManager?.endCommunication()
        mRtcEngine?.leaveChannel()
        if (isCallWasOnGoing) {
            isCallWasOnGoing = false
            disconnectService()
        } else {
            callStopWithoutIssue()
        }
    }

    fun answerCall(data: HashMap<String, String?>) {
        executor.execute {
            try {
                stopRing()
                joinCall(data)
                executeEvent(AnalyticsEvent.USER_ANSWER_EVENT_P2P.NAME)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun joinOutgoingCall(data: HashMap<String, String?>) {
        executor.execute {
            try {
                isTimeOutToPickCall = false
                callStartTime = 0L
                data.let {
                    callData = it
                }
                callType = CallType.FAVORITE_OUTGOING
                joinCall(data)
                executeEvent(AnalyticsEvent.OUTGOING_CALL.NAME)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }


    private fun joinCall(data: HashMap<String, String?>) {
        if (isTimeOutToPickCall) {
            isTimeOutToPickCall = false
            RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
            return
        }
        if (callData == null) {
            callData = data
        }
        data.printAll()
        val statusCode = mRtcEngine?.joinChannel(
            getToken(data),
            getChannelName(data), "test",
            getUID(data)
        ) ?: -3
        if (callForceDisconnect) {
            mRtcEngine?.leaveChannel()
        }
        Timber.tag(TAG).e("ha join$statusCode")

        if (statusCode < 0) {
            if (retryInitLibrary == 3) {
                disconnectCall()
                return
            }
            retryInitLibrary++
            try {
                Thread.sleep(350)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            initEngine {
                joinCall(data)
            }
        }
        isCallWasOnGoing = true
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

    private fun getCallerUID(): Int {
        return callData?.get(RTC_CALLER_UID_KEY)?.toInt() ?: 0
    }

    private fun getCallerUrl(): String? {
        return callData?.get(RTC_CALLER_PHOTO)
    }

    private fun isFavorite(): Boolean {
        return callData?.containsKey(RTC_IS_FAVORITE) == true
    }

    private fun getCallerName(): String {
        return callData?.get(RTC_NAME) ?: getString(R.string.favorite_p2p_title)
    }

    fun getSpeaker() = isSpeakerEnable

    fun getMic() = isMicEnable

    fun getUserAgoraId() = userAgoraId

    fun getCallId() = callId

    private fun muteCall() {
        mRtcEngine?.muteLocalAudioStream(true)
    }

    private fun unMuteCall() {
        mRtcEngine?.muteLocalAudioStream(false)
    }

    fun startCallTimer() {
        callStartTime = SystemClock.elapsedRealtime()
    }

    fun getTimeOfTalk(): Long {
        return if (callStartTime == 0L) {
            0
        } else SystemClock.elapsedRealtime() - callStartTime
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun switchAudioSpeaker() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        isSpeakerEnable = !isSpeakerEnable
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        mRtcEngine?.setEnableSpeakerphone(isSpeakerEnable)
        mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(isSpeakerEnable)
        audioManager.isSpeakerphoneOn = isSpeakerEnable
    }

    fun switchSpeck() {
        try {
            isMicEnable = !isMicEnable
            if (isMicEnable) {
                unMuteCall()
            } else {
                muteCall()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    private fun resetConfig() {
        stopRing()
        joshAudioManager?.stopConnectTone()
        removeSensor()
        isCallerJoin = false
        eventListener = null
        isCallRecordOngoing = false
        isSpeakerEnable = false
        isMicEnable = true
        phoneCallState = CallState.CALL_STATE_IDLE
        compositeDisposable.clear()
    }

    private fun disconnectService() {
        resetConfig()
        removeNotifications()
    }

    private fun removeNotifications() {
        try {
            mNotificationManager?.cancelAll()
            stopForeground(true)
            addMissCallNotification()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun addMissCallNotification() {
        if (isFavorite() && isTimeOutToPickCall) {
            NotificationUtil(this).addMissCallPPNotification(
                callData,
                getCallerUID()
            )
        }
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        RtcEngine.destroy()
        AppObjectController.mRtcEngine = null
        joshAudioManager?.quitEverything()
        handlerThread?.quitSafely()
        isEngineInit = false
        isTimeOutToPickCall = false
        isCallRecordOngoing = false
        switchChannel = false
        isCallerJoin = false
        callStartTime = 0L
        retryInitLibrary = 0
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).e("OnTaskRemoved")
    }

    override fun onDestroy() {
        RtcEngine.destroy()
        stopRing()
        isEngineInit = false
        joshAudioManager?.quitEverything()
        AppObjectController.mRtcEngine = null
        handlerThread?.quitSafely()
        isTimeOutToPickCall = false
        isCallerJoin = false
        callStartTime = 0L
        isCallRecordOngoing = false
        retryInitLibrary = 0
        isCallWasOnGoing = false
        switchChannel = false
        TelephonyUtil.getManager(this)
            .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_NONE)
        phoneCallState = CallState.CALL_STATE_IDLE
        Timber.tag(TAG).e("onDestroy")
        removeSensor()
        executor.shutdown()
        super.onDestroy()
    }

    private fun addNotification(action: String, data: HashMap<String, String?>?) {
        //mNotificationManager?.cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (action) {
                IncomingCall().action, FavoriteIncomingCall().action -> {
                    showNotification(incomingCallNotification(data), INCOMING_CALL_NOTIFICATION_ID)
                    startRingtoneAndVibration()
                }
                CallConnect().action -> {
                    showNotification(
                        callConnectedNotification(data),
                        CONNECTED_CALL_NOTIFICATION_ID
                    )
                }
                CallForceConnect().action -> {
                    showNotification(actionNotification("Connecting Call"), ACTION_NOTIFICATION_ID)
                }
                CallDisconnect().action -> {
                    showNotification(
                        actionNotification("Disconnecting Call"),
                        ACTION_NOTIFICATION_ID
                    )
                }
            }
        }
    }

    private fun showNotification(notification: Notification, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun incomingCallNotification(incomingData: HashMap<String, String?>?): Notification {
        Timber.tag(TAG).e("incomingCallNotification   ")

        val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            uniqueInt, getWebRtcActivityIntent(CallType.INCOMING),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.p2p_title))
            .setContentText("Incoming voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setContentIntent(pendingIntent)
            .setChannelId(CALL_NOTIFICATION_CHANNEL)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip Incoming Call"
            var chanIndex: Int = PrefManager.getIntValue("calls_notification_channel")
            val oldChannel =
                mNotificationManager?.getNotificationChannel("incoming_calls$chanIndex")
            if (oldChannel != null) {
                mNotificationManager?.deleteNotificationChannel(oldChannel.id)
            }
            val existingChannel =
                mNotificationManager?.getNotificationChannel("incoming_calls2$chanIndex")
            var needCreate = true
            if (existingChannel != null) {
                if (existingChannel.importance < IMPORTANCE_HIGH || existingChannel.vibrationPattern != null || existingChannel.shouldVibrate()) {
                    mNotificationManager?.deleteNotificationChannel("incoming_calls2$chanIndex")
                    chanIndex++
                    PrefManager.put("calls_notification_channel", chanIndex)
                } else {
                    needCreate = false
                }
            }
            if (needCreate) {
                val chan = NotificationChannel(
                    "incoming_calls2$chanIndex", name,
                    IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for voice calling"
                }
                chan.setSound(null, null)
                chan.enableVibration(false)
                chan.enableLights(false)
                chan.setBypassDnd(true)
                chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                try {
                    mNotificationManager?.createNotificationChannel(chan)
                } catch (e: java.lang.Exception) {
                    this.stopSelf()
                }
            }
            builder.setChannelId("incoming_calls2$chanIndex")
        } else {
            builder.setSound(null, AudioManager.STREAM_RING)
        }

        val declineActionIntent =
            Intent(AppObjectController.joshApplication, WebRtcService::class.java)
        declineActionIntent.action = CallReject().action
        val declinePendingIntent: PendingIntent =
            PendingIntent.getService(
                this,
                0,
                declineActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_call_end,
                getActionText(R.string.hang_up, R.color.error_color),
                declinePendingIntent
            )
        )

        val answerActionIntent =
            Intent(AppObjectController.joshApplication, WebRtcService::class.java)
                .apply {
                    action = CallConnect().action
                    putExtra(CALL_USER_OBJ, incomingData)
                }

        val answerPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, answerActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_pick_call,
                getActionText(R.string.answer, R.color.action_color),
                answerPendingIntent
            )
        )

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setFullScreenIntent(
            FullScreenActivity.getPendingIntent(this, 22),
            true
        )
        val avatar: Bitmap? = getIncomingCallAvatar(isFavorite = isFavorite())
        val customView = getRemoteViews(isFavorite = isFavorite())

        customView.setImageViewBitmap(R.id.photo, avatar)
        customView.setOnClickPendingIntent(R.id.answer_btn, answerPendingIntent)
        customView.setOnClickPendingIntent(R.id.decline_btn, declinePendingIntent)
        builder.setLargeIcon(avatar)
        builder.setCustomHeadsUpContentView(customView)
        builder.setCustomBigContentView(customView)
        builder.setCustomContentView(customView)
        builder.setSound(null, AudioManager.STREAM_VOICE_CALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = IMPORTANCE_HIGH
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVibrate(LongArray(0))
            builder.setCategory(Notification.CATEGORY_CALL)
        }

        builder.setShowWhen(false)
        return builder.build()
    }


    private fun getNameForImage(): String {
        return try {
            callData?.get(RTC_NAME)?.substring(0, 2) ?: getRandomName()
        } catch (ex: Exception) {
            getRandomName()
        }
    }

    private fun getIncomingCallAvatar(isFavorite: Boolean): Bitmap? {
        return if (getCallerUrl() == null) {
            getNameForImage().textDrawableBitmap(width = 80, height = 80)
        } else {
            if (isFavorite) {
                getCallerUrl()?.urlToBitmap()
            } else {
                getRandomName().textDrawableBitmap()
            }
        }
    }

    private fun getRemoteViews(isFavorite: Boolean): RemoteViews {
        val layout = if (isFavorite) {
            R.layout.favorite_call_notification
        } else {
            R.layout.call_notification
        }
        val customView = RemoteViews(packageName, layout)
        customView.setTextViewText(
            R.id.name, if (isFavorite) {
                getString(R.string.favorite_p2p_title)
            } else {
                getString(R.string.p2p_title)
            }
        )
        customView.setTextViewText(
            R.id.title, if (isFavorite) {
                getCallerName()
            } else {
                getString(R.string.p2p_subtitle)
            }
        )

        customView.setTextViewText(
            R.id.answer_text, getActionText(
                R.string.answer,
                R.color.action_color
            )
        )
        customView.setTextViewText(
            R.id.decline_text, getActionText(
                R.string.hang_up,
                R.color.error_color
            )
        )
        return customView
    }

    private fun callConnectedNotification(data: HashMap<String, String?>?): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip call connect"
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CALL_NOTIFICATION_CHANNEL, name, importance).apply {
                description = "Notifications for voice calling"
            }
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            uniqueInt, getWebRtcActivityIntent(CallType.INCOMING),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val declineActionIntent =
            Intent(AppObjectController.joshApplication, WebRtcService::class.java)
        declineActionIntent.action = CallDisconnect().action

        val declineActionPendingIntent: PendingIntent =
            PendingIntent.getService(
                this,
                0,
                declineActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        val lNotificationBuilder = NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL)
            .setChannelId(CALL_NOTIFICATION_CHANNEL)
            .setContentIntent(pendingIntent)
            .setContentTitle(getNameAfterConnectedCall(data))
            .setContentText("Ongoing voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            .setOngoing(true)
            .setContentInfo("Outgoing call")
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_call_end,
                    getActionText(R.string.hang_up, R.color.error_color),
                    declineActionPendingIntent
                )
            ).setStyle(
                androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0)
            )
        return lNotificationBuilder.build()
    }

    private fun actionNotification(title: String = ""): Notification {
        Timber.tag(TAG).e("actionNotification  ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelName: CharSequence = "Voip Call Status"
            val mChannel = NotificationChannel(
                CALL_NOTIFICATION_CHANNEL,
                notificationChannelName,
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Notifications for voice calling"
            }
            mNotificationManager?.createNotificationChannel(mChannel)
        }
        val lNotificationBuilder = NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL)
            .setChannelId(CALL_NOTIFICATION_CHANNEL)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setProgress(0, 0, true)
        lNotificationBuilder.priority = NotificationCompat.PRIORITY_MAX
        return lNotificationBuilder.build()
    }

    private fun getWebRtcActivityIntent(callType: CallType): Intent {
        return Intent(
            this,
            WebRtcActivity::class.java
        ).apply {
            putExtra(CALL_TYPE, callType)
            putExtra(CALL_USER_OBJ, callData)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getNameAfterConnectedCall(header: HashMap<String, String?>?): String {
        return "Speaking Practice"
    }

    private fun getActionText(@StringRes stringRes: Int, @ColorRes colorRes: Int): Spannable {
        val spannable: Spannable = SpannableString(getText(stringRes))
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, colorRes)),
            0,
            spannable.length,
            0
        )
        return spannable
    }

    private fun callStatusNetworkApi(
        data: HashMap<String, String?>,
        callAction: CallAction,
        hasDisconnected: Boolean = false
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val time = getTimeOfTalk()
                data["mentor_id"] = Mentor.getInstance().getId()
                data["call_response"] = callAction.action
                data["duration"] = TimeUnit.MILLISECONDS.toSeconds(time).toString()
                data["has_disconnected"] = hasDisconnected.toString()

                var resp = AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
                if (resp.code() == 500) {
                    callCallback?.get()?.onNoUserFound()
                }
                if (CallAction.ACCEPT == callAction) {
                    callCallback?.get()?.onServerConnect()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


}

sealed class WebRtcCalling
data class InitLibrary(val action: String = "calling.action.initLibrary") : WebRtcCalling()
data class IncomingCall(val action: String = "calling.action.incoming_call") : WebRtcCalling()
data class CallConnect(val action: String = "calling.action.connect") : WebRtcCalling()
data class CallDisconnect(val action: String = "calling.action.disconnect") : WebRtcCalling()
data class CallReject(val action: String = "calling.action.callReject") : WebRtcCalling()

data class OutgoingCall(val action: String = "calling.action.outgoing_call") : WebRtcCalling()
data class CallStop(val action: String = "calling.action.stopcall") : WebRtcCalling()
data class CallForceConnect(val action: String = "calling.action.force_connect") : WebRtcCalling()
data class CallForceDisconnect(val action: String = "calling.action.force_disconnect") :
    WebRtcCalling()

data class NoUserFound(val action: String = "calling.action.no_user_found") :
    WebRtcCalling()

data class HoldCall(val action: String = "calling.action.hold_call") : WebRtcCalling()
data class ResumeCall(val action: String = "calling.action.resume_call") : WebRtcCalling()

data class FavoriteIncomingCall(val action: String = "calling.action.favorite_incoming_call") :
    WebRtcCalling()


enum class CallState(val state: Int) {
    CALL_STATE_CONNECTED(0), CALL_STATE_IDLE(1), CALL_STATE_BUSY(2),
    CONNECT(3), DISCONNECT(4), REJECT(5), ONHOLD(6), UNHOLD(7), EXIT(8),
    WAITING_FOR_NETWORK(9), CALL_HOLD_BY_OPPOSITE(10), CALL_RESUME_BY_OPPOSITE(11)
}

enum class CallAction(val action: String) {
    ACCEPT("ACCEPT"), DECLINE("DECLINE"), DISCONNECT("DISCONNECT"), TIMEOUT("TIMEOUT"),
    ONHOLD("ONHOLD"), RESUME("RESUME"), NOUSERFOUND("NOUSERFOUND"),
    AUTO_DISCONNECT("AUTO_DISCONNECT")
}

class NotificationId {
    companion object {
        const val ACTION_NOTIFICATION_ID = 200000
        const val INCOMING_CALL_NOTIFICATION_ID = 200001
        const val CONNECTED_CALL_NOTIFICATION_ID = 200002
        const val CALL_NOTIFICATION_CHANNEL = "Call Notifications"
    }
}

interface WebRtcCallback {
    fun onChannelJoin() {}
    fun onConnect(callId: String) {}
    fun onDisconnect(callId: String?, channelName: String?, time: Long = 0) {}
    fun onCallReject(callId: String?) {}
    fun switchChannel(data: HashMap<String, String?>) {}
    fun onNoUserFound() {}
    fun onServerConnect() {}
    fun onIncomingCall() {}
    fun onNetworkLost() {}
    fun onNetworkReconnect() {}
    fun onHoldCall() {}
    fun onUnHoldCall() {}
}