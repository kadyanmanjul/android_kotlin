package com.joshtalks.joshskills.ui.voip

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.SystemClock
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CallType
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_FOREGROUND
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.firestore.AgoraNotificationListener
import com.joshtalks.joshskills.core.firestore.FirestoreDB
import com.joshtalks.joshskills.core.getRandomName
import com.joshtalks.joshskills.core.notification.FirebaseNotificationService
import com.joshtalks.joshskills.core.printAll
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.core.startServiceForWebrtc
import com.joshtalks.joshskills.core.textDrawableBitmap
import com.joshtalks.joshskills.core.urlToBitmap
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationObject
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.ACTION_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CALL_NOTIFICATION_CHANNEL
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.util.NotificationUtil
import com.joshtalks.joshskills.ui.voip.util.TelephonyUtil
import io.agora.rtc.Constants
import io.agora.rtc.Constants.AUDIO_PROFILE_SPEECH_STANDARD
import io.agora.rtc.Constants.AUDIO_ROUTE_HEADSET
import io.agora.rtc.Constants.AUDIO_ROUTE_HEADSETBLUETOOTH
import io.agora.rtc.Constants.AUDIO_SCENARIO_EDUCATION
import io.agora.rtc.Constants.CHANNEL_PROFILE_COMMUNICATION
import io.agora.rtc.Constants.CONNECTION_CHANGED_INTERRUPTED
import io.agora.rtc.Constants.CONNECTION_STATE_RECONNECTING
import io.agora.rtc.Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY
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
const val DEFAULT_NOTIFICATION_TITLE = "Josh Skills App Running"

class WebRtcService : BaseWebRtcService() {
    private val TAG = "WebRtcService"
    private val mBinder: IBinder = MyBinder()
    private val hangUpRtcOnDeviceCallAnswered: PhoneStateListener =
        HangUpRtcOnPstnCallAnsweredListener()

    private var callStartTime: Long = 0
    private var callForceDisconnect = false
    private var mHandler: Handler? = null
    private val handlerThread: HandlerThread by lazy { CustomHandlerThread("WebrtcThread") }
    private var userAgoraId: Int? = null
    var channelName: String? = null
    private var isEngineInitialized = false
    var isCallerJoined: Boolean = false
    private var isMicEnabled = true
    private var isSpeakerEnabled = false
    private var isSpeakerTurningOn = false
    var isBluetoothEnabled = false
        private set
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val AUDIO_SWITCH_OFFSET = 1000L
    private var oppositeCallerId: Int? = null
    private var userDetailMap: HashMap<String, String>? = null
    private var notificationState = NotificationState.NOT_VISIBLE

    companion object {
        private val TAG = WebRtcService::class.java.simpleName
        var pstnCallState = CallState.CALL_STATE_IDLE

        var isOnPstnCall = false

        var BLUETOOTH_RETRY_COUNT = 0
        var currentButtonState = VoipButtonState.NONE

        @JvmStatic
        private val callReconnectTime = AppObjectController.getFirebaseRemoteConfig()
            .getLong(FirebaseRemoteConfigKey.VOIP_CALL_RECONNECT_TIME)

        @JvmStatic
        private val callDisconnectTime = AppObjectController.getFirebaseRemoteConfig()
            .getLong(FirebaseRemoteConfigKey.VOIP_CALL_DISCONNECT_TIME)

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
        var isCallOnGoing: MutableLiveData<Boolean> = MutableLiveData(false)

        @Volatile
        var holdCallByMe: Boolean = false

        @Volatile
        var holdCallByAnotherUser: Boolean = false

        @Volatile
        var retryInitLibrary: Int = 0

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

        fun userJoined(uid: Int) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = UserJoined().action
                putExtra(OPPOSITE_USER_UID, uid)
            }
            serviceIntent.startServiceForWebrtc()
        }
    }

    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
                when (routing) {
                    AUDIO_ROUTE_HEADSETBLUETOOTH -> {
                        VoipAudioState.switchToBluetooth()
                    }
                    Constants.AUDIO_ROUTE_LOUDSPEAKER -> {
                        VoipAudioState.switchToSpeaker()
                    }
                    Constants.AUDIO_ROUTE_SPEAKERPHONE -> {
                        VoipAudioState.switchToSpeaker()
                    }
                    else -> {
                        VoipAudioState.switchToDefault(am.isWiredHeadsetOn)
                    }
                }
        }

        override fun onError(errorCode: Int) {
            Timber.tag(TAG).e("onError=  $errorCode")
            super.onError(errorCode)
            if (switchChannel) {
                switchChannel = false
                return
            }
            if (isCallOnGoing.value == true) {
                return
            }
            RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
            disconnectService()
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Timber.tag(TAG).e("onJoinChannelSuccess=  $channel = $uid   ")
            removeIncomingNotification()
            compositeDisposable.clear()
            userAgoraId = uid
            isCallOnGoing.postValue(true)
            callData?.let {
                channelName = getChannelName(it)
                try {
                    val id = getUID(it)
                    if ((callType == CallType.INCOMING || callType == CallType.FAVORITE_INCOMING) && id == uid) {
                        startCallTimer()
                        callStatusNetworkApi(it, CallAction.ACCEPT)
                        addNotification(CallConnect().action, callData)
                        //addSensor()
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
            isCallOnGoing.postValue(false)
            if (switchChannel.not()) {
                callCallback?.get()?.onDisconnect(
                    callId,
                    callData?.let { getChannelName(it) },
                    if (isCallerJoined) {
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
            userJoined(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Timber.tag(TAG).e("onUserOffline=  $uid  $reason")
            super.onUserOffline(uid, reason)
            callData?.let {
                val id = getUID(it)
                Timber.tag(TAG).e("onUserOffline =  $id")
                if (id != uid && reason == Constants.USER_OFFLINE_QUIT) {
                    if (isCallerJoined) {
                        endCall(apiCall = true, action = CallAction.DISCONNECT)
                    } else {
                        endCall(apiCall = true, action = CallAction.AUTO_DISCONNECT)
                    }
                    isCallOnGoing.postValue(false)
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
            if (pstnCallState == CallState.CALL_STATE_IDLE) {
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

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            super.onConnectionStateChanged(state, reason)
            Timber.tag(TAG).e("onConnectionStateChanged    $state     $reason")
            if (CONNECTION_STATE_RECONNECTING == state && reason == CONNECTION_CHANGED_INTERRUPTED) {
                compositeDisposable.add(
                    Completable.complete()
                        .delay(5, TimeUnit.SECONDS)
                        .doOnComplete {
                            Timber.tag("Reconnect").e("doOnComplete  $isCallerJoined")
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            if (isCallerJoined) {
                                lostNetwork()
                            }
                        }
                )
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
            mHandler = Handler(handlerThread.looper) { msg ->
                when (msg.what) {
                    CallState.UNHOLD.state -> {
                        pstnCallState = CallState.CALL_STATE_IDLE
                        if (holdCallByMe) {
                            mRtcEngine?.connectionState
                            callData?.let {
                                callStatusNetworkApi(it, CallAction.RESUME)
                            }
                        }
                        holdCallByMe = false
                        mRtcEngine?.muteAllRemoteAudioStreams(false)
                        mRtcEngine?.muteLocalAudioStream(false)
                        mRtcEngine?.enableLocalAudio(true)
                        callCallback?.get()?.onUnHoldCall()
                    }
                    CallState.ONHOLD.state -> {
                        pstnCallState = CallState.CALL_STATE_CONNECTED
                        holdCallByMe = true
                        mRtcEngine?.muteAllRemoteAudioStreams(true)
                        mRtcEngine?.muteLocalAudioStream(true)
                        mRtcEngine?.enableLocalAudio(false)
                        callCallback?.get()?.onHoldCall()
                        callData?.let {
                            callStatusNetworkApi(it, CallAction.ONHOLD)
                        }
                    }
                    CallState.EXIT.state -> {
                        RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                    }
                    CallState.CALL_HOLD_BY_OPPOSITE.state -> {
                        holdCallByAnotherUser = true
                        mRtcEngine?.muteAllRemoteAudioStreams(true)
                        mRtcEngine?.muteLocalAudioStream(true)
                        mRtcEngine?.enableLocalAudio(false)
                        callCallback?.get()?.onHoldCall()
                    }
                    CallState.CALL_RESUME_BY_OPPOSITE.state -> {
                        holdCallByAnotherUser = false
                        mRtcEngine?.muteAllRemoteAudioStreams(false)
                        mRtcEngine?.muteLocalAudioStream(false)
                        mRtcEngine?.enableLocalAudio(true)
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
                    isOnPstnCall = false
                    pstnCallState = CallState.CALL_STATE_IDLE
                    mRtcEngine?.muteAllRemoteAudioStreams(false)
                    mRtcEngine?.muteLocalAudioStream(false)
                    mRtcEngine?.enableLocalAudio(true)

                    mRtcEngine?.adjustRecordingSignalVolume(400)
                    val audio = getSystemService(AUDIO_SERVICE) as AudioManager
                    val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                    val currentVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                    mRtcEngine?.adjustPlaybackSignalVolume((95 / maxVolume) * currentVolume)

                    val message = Message()
                    message.what = CallState.UNHOLD.state
                    mHandler?.sendMessageDelayed(message, 500)
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    isOnPstnCall = true
                    val message = Message()
                    message.what = CallState.ONHOLD.state
                    mHandler?.sendMessage(message)
                }
                else -> {
                    isOnPstnCall = true
                    pstnCallState = CallState.CALL_STATE_BUSY
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
        initIncomingCallChannel()
        pstnCallState = CallState.CALL_STATE_IDLE
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
        CoroutineScope(Dispatchers.IO).launch {
            TelephonyUtil.getManager(this@WebRtcService)
                .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_CALL_STATE)
        }
        addFirestoreObserver()
    }

    private fun addFirestoreObserver() {
        FirestoreDB.setNotificationListener(listener = object : AgoraNotificationListener {
            override fun onReceived(firestoreNotification: FirestoreNotificationObject) {
                val nc = firestoreNotification.toNotificationObject(null)
                FirebaseNotificationService.sendFirestoreNotification(nc, this@WebRtcService)
            }
        })
    }

    private fun initIncomingCallChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            executor.execute {
                val chanIndex: Int = PrefManager.getIntValue("calls_notification_channel")
                if (chanIndex == 0) {
                    val importance =
                        if (canHeadsUpNotification()) IMPORTANCE_HIGH else IMPORTANCE_LOW
                    val name: CharSequence = "Voip Incoming Call"
                    val chan = NotificationChannel("incoming_calls2$chanIndex", name, importance)
                    try {
                        mNotificationManager?.createNotificationChannel(chan)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
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
                if (BuildConfig.DEBUG) {
                    setParameters("{\"rtc.log_filter\": 65535}")
                    setParameters("{\"che.audio.start_debug_recording\":\"all\"}")
                }
                setParameters("{\"rtc.peer.offline_period\":$callReconnectTime}")
                setParameters("{\"che.audio.keep.audiosession\":true}")
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)

                disableVideo()
                enableAudio()
                enableAudioVolumeIndication(1000, 3, true)
                setAudioProfile(
                    AUDIO_PROFILE_SPEECH_STANDARD,
                    AUDIO_SCENARIO_EDUCATION
                )
                setChannelProfile(CHANNEL_PROFILE_COMMUNICATION)
                adjustRecordingSignalVolume(400)
                val audio = getSystemService(AUDIO_SERVICE) as AudioManager
                val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val currentVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                adjustPlaybackSignalVolume((95 / maxVolume) * currentVolume)
                enableDeepLearningDenoise(true)
                // Configuration for the publisher. When the network condition is poor, send audio only.
                setLocalPublishFallbackOption(STREAM_FALLBACK_OPTION_AUDIO_ONLY)

                // Configuration for the subscriber. Try to receive low stream under poor network conditions. When the current network conditions are not sufficient for video streams, receive audio stream only.
                setRemoteSubscribeFallbackOption(STREAM_FALLBACK_OPTION_AUDIO_ONLY)
            }
            if (mRtcEngine != null) {
                isEngineInitialized = true
                callback.invoke()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand=  %s", intent?.action)
        val isForeground = intent?.extras?.getBoolean(IS_FOREGROUND, false)
        Timber.tag(TAG).e("onStartCommand: is Foreground --> $isForeground")
        if (isForeground == true && notificationState == NotificationState.NOT_VISIBLE) {
            showDefaultNotification()
        }
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
                                if (CallState.CALL_STATE_BUSY == pstnCallState || isCallOnGoing.value == true) {
                                    return@initEngine
                                }
                                val data =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                data.let {
                                    callData = it
                                }
                                setOppositeUserInfo(null)
                                callType = CallType.INCOMING
                                isTimeOutToPickCall = false
                                callStartTime = 0L
                                handleIncomingCall()
                            }
                            this == OutgoingCall().action -> {
                                setOppositeUserInfo(null)
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
                                removeIncomingNotification()
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
                                stopRing()
                                callForceDisconnect = true
                                if (JoshApplication.isAppVisible.not()) {
                                    addNotification(CallDisconnect().action, null)
                                }
                                endCall(apiCall = false)
                                RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                            }
                            this == CallForceConnect().action -> {
                                stopRing()
                                callStartTime = 0L
                                compositeDisposable.clear()
                                switchChannel = true
                                setOppositeUserInfo(null)
                                if (isCallOnGoing.value == true) {
                                    mRtcEngine?.leaveChannel()
                                }
                                resetConfig()
                                addNotification(CallForceConnect().action, null)
                                callData = null
                                AppObjectController.uiHandler.postDelayed(
                                    {
                                        val data =
                                            intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                        callData = data
                                        if (data.containsKey(RTC_CHANNEL_KEY)) {
                                            channelName = data[RTC_CHANNEL_KEY]
                                        }
                                        removeIncomingNotification()
                                        if (callCallback != null && callCallback?.get() != null) {
                                            callCallback?.get()?.switchChannel(data)
                                        } else {
                                            startAutoPickCallActivity(false)
                                        }
                                    },
                                    750
                                )
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
                            this == UserJoined().action -> {
                                val uid =
                                    intent.getIntExtra(OPPOSITE_USER_UID, -1)
                                if (uid != -1) {
                                    userJoined(uid)
                                }
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

    fun userJoined(uid: Int) {
        removeIncomingNotification()
        oppositeCallerId = uid
        compositeDisposable.clear()
        isCallOnGoing.postValue(true)
        isCallerJoined = true
        if (callStartTime == 0L) {
            startCallTimer()
        }
        callCallback?.get()?.onConnect(uid.toString())
        mHandler?.postDelayed(
            {
                callCallback?.get()?.onServerConnect()
            },
            500
        )
        addNotification(CallConnect().action, callData)
        joshAudioManager?.startCommunication()
        joshAudioManager?.stopConnectTone()
        audioFocus()
    }

    private fun audioFocus() {
        Log.d("AUDIO", "audioFocus: ")
        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val af = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(
                    AudioAttributes.Builder().run {
                        setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    }
                )
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

    private fun callConnectService(data: HashMap<String, String?>?) {
        data?.let {
            callData = it
        }
        removeIncomingNotification()
        startAutoPickCallActivity(true)
    }

    fun removeIncomingNotification() {
        mNotificationManager?.cancel(ACTION_NOTIFICATION_ID)
        mNotificationManager?.cancel(INCOMING_CALL_NOTIFICATION_ID)
    }

    private fun startAutoPickCallActivity(autoPick: Boolean) {
        val callActivityIntent =
            Intent(
                this,
                WebRtcActivity::class.java
            ).apply {
                callData?.apply {
                    if (isFavorite()) {
                        put(RTC_IS_FAVORITE, "true")
                    }
                }
                putExtra(CALL_TYPE, CallType.INCOMING)
                putExtra(AUTO_PICKUP_CALL, autoPick)
                putExtra(CALL_USER_OBJ, callData)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(callActivityIntent)
    }

    fun openConnectedCallActivity(context: Activity) {
        val callActivityIntent = Intent(context, WebRtcActivity::class.java).apply {
            callData?.apply {
                if (isFavorite()) {
                    put(RTC_IS_FAVORITE, "true")
                }
            }
            putExtra(CALL_TYPE, callType)
            putExtra(IS_CALL_CONNECTED, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(callActivityIntent)
        context.overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_exit)
    }

    private fun handleIncomingCall() {
        executeEvent(AnalyticsEvent.INIT_CALL.NAME)
        callData?.let {
            if (canHeadsUpNotification().not()) {
                showIncomingCallScreen(it)
            }
        }
        addNotification(IncomingCall().action, callData)
        addTimeObservable()
    }

    private fun showIncomingCallScreen(
        data: HashMap<String, String?>,
        autoPickupCall: Boolean = false
    ) {
        val callActivityIntent = Intent(this, WebRtcActivity::class.java).apply {
            data.apply {
                if (isFavorite()) {
                    put(RTC_IS_FAVORITE, "true")
                }
            }
            putExtra(CALL_USER_OBJ, data)
            putExtra(CALL_TYPE, CallType.INCOMING)
            putExtra(AUTO_PICKUP_CALL, autoPickupCall)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(callActivityIntent)
    }

    private fun addTimeObservable() {
        compositeDisposable.add(
            Completable.complete()
                .delay(10, TimeUnit.SECONDS)
                .doOnComplete {
                    if (isCallConnected().not()) {
                        isTimeOutToPickCall = true
                        disconnectCallFromCallie()
                    }
                }
                .subscribe()
        )
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
                }
        )
    }

    fun isCallConnected(): Boolean {
        return ((mRtcEngine?.connectionState == Constants.CONNECTION_STATE_CONNECTING ||
                mRtcEngine?.connectionState == Constants.CONNECTION_STATE_CONNECTED ||
                mRtcEngine?.connectionState == Constants.CONNECTION_STATE_RECONNECTING) &&
                isCallOnGoing.value == true &&
                isCallerJoined)
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
        if (isCallOnGoing.value == true) {
            isCallOnGoing.postValue(false)
            disconnectService()
        } else {
            callStopWithoutIssue()
        }
    }

    fun setOngoingCall() {
        isCallOnGoing.postValue(false)
    }

    private fun showDefaultNotification() {
        showNotification(actionNotification(DEFAULT_NOTIFICATION_TITLE), ACTION_NOTIFICATION_ID)
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
        isCallOnGoing.postValue(true)
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

    fun isFavorite(): Boolean {
        if (callData != null && callData!!.containsKey(RTC_IS_FAVORITE)) {
            return true
        }
        return false
    }

    fun setAsFavourite() {
        callData?.put(RTC_IS_FAVORITE, "true")
    }

    private fun getCallerName(): String {
        return callData?.get(RTC_NAME) ?: EMPTY
    }

    fun getSpeaker() = isSpeakerEnabled

    fun getMic() = isMicEnabled

    fun getUserAgoraId() = userAgoraId

    fun getCallId() = callId

    fun getOppositeCallerId() = oppositeCallerId

    fun getOppositeCallerName() = userDetailMap?.get("name")

    fun getOppositeCallerProfilePic() = userDetailMap?.get("profile_pic")

    fun setOppositeUserInfo(obj: HashMap<String, String>?) {
        userDetailMap = obj
    }

    fun getOppositeUserInfo() = userDetailMap

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

    fun getCallType() = callType

    override fun onBind(intent: Intent): IBinder {
        Timber.tag(TAG).e("onBind")
        return mBinder
    }

    @Synchronized
    fun turnOnDefault(state: VoipButtonState) {
        Log.d(TAG, "turnOnDefault: ")
        if (state != VoipButtonState.BLUETOOTH)
            BLUETOOTH_RETRY_COUNT = 0
        val am: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        currentButtonState = state
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        am.stopBluetoothSco()
        am.isBluetoothScoOn = false
        if (state == VoipButtonState.DEFAULT)
            showToast("Switching to ${if (am.isWiredHeadsetOn) "Earphone" else "Headset"}...")
        AppObjectController.uiHandler.postDelayed({
            mRtcEngine?.setEnableSpeakerphone(false)
            am.isSpeakerphoneOn = false
            if (state == VoipButtonState.BLUETOOTH)
                turnOnBluetooth(state, isRetrying = true)
            else
                VoipAudioState.switchToDefault(am.isWiredHeadsetOn)
        }, AUDIO_SWITCH_OFFSET)
    }

    @Synchronized
    fun turnOnBluetooth(state: VoipButtonState, isRetrying: Boolean = false) {
        Log.d(TAG, "turnOnBluetooth: ")
        if (!isRetrying && currentButtonState == state) {
            BLUETOOTH_RETRY_COUNT += 1
            retryTurningOnBluetooth(state)
            return
        }
        currentButtonState = state
        val am: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        mRtcEngine?.setEnableSpeakerphone(false)
        am.isSpeakerphoneOn = false
        if (BLUETOOTH_RETRY_COUNT == 0)
            showToast("Switching to Bluetooth...")
        else if (BLUETOOTH_RETRY_COUNT < 3)
            showToast("Retrying($BLUETOOTH_RETRY_COUNT) switching to Bluetooth")
        else if (BLUETOOTH_RETRY_COUNT >= 3)
            showToast("Please restart your bluetooth headset")

        AppObjectController.uiHandler.postDelayed({
            am.startBluetoothSco()
            am.isBluetoothScoOn = true
            VoipAudioState.switchToBluetooth()
        }, AUDIO_SWITCH_OFFSET)
    }

    @Synchronized
    private fun retryTurningOnBluetooth(state: VoipButtonState) {
        when (BLUETOOTH_RETRY_COUNT) {
            1 -> {
                turnOnSpeaker(state)
            }
            2 -> {
                turnOnDefault(state)
            }
            else -> {
                turnOnBluetooth(state, true)
            }
        }
    }

    @Synchronized
    fun turnOnSpeaker(state: VoipButtonState) {
        Log.d(TAG, "turnOnSpeaker: ")
        if (state != VoipButtonState.BLUETOOTH)
            BLUETOOTH_RETRY_COUNT = 0
        currentButtonState = state
        val am: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        am.stopBluetoothSco()
        am.isBluetoothScoOn = false
        if (state == VoipButtonState.SPEAKER)
            showToast("Switching to Speaker...")
        AppObjectController.uiHandler.postDelayed({
            mRtcEngine?.setEnableSpeakerphone(true)
            am.isSpeakerphoneOn = true
            if (state == VoipButtonState.BLUETOOTH)
                turnOnBluetooth(state, isRetrying = true)
            else
                VoipAudioState.switchToSpeaker()
        }, AUDIO_SWITCH_OFFSET)
    }

    /*override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
        Timber.tag("BLUETOOTH").d("bluetoothConnected")
    }

    override fun onServiceDisconnected(profile: Int) {
        if (currentButtonState == VoipButtonState.BLUETOOTH && BLUETOOTH_RETRY_COUNT == 3) {
            AppObjectController.uiHandler.postDelayed({
                bluetoothAdapter?.enable()
            }, 1000)
            return
        }
    }*/

    fun switchSpeck() {
        executor.submit {
            try {
                if (holdCallByMe.not() && holdCallByAnotherUser.not()) {
                    isMicEnabled = !isMicEnabled
                    if (isMicEnabled) {
                        unMuteCall()
                    } else {
                        muteCall()
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun resetConfig() {
        stopRing()
        joshAudioManager?.stopConnectTone()
        isCallerJoined = false
        eventListener = null
        isSpeakerEnabled = false
        isMicEnabled = true
        oppositeCallerId = null
        pstnCallState = CallState.CALL_STATE_IDLE
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
            notificationState = NotificationState.NOT_VISIBLE
            addMissCallNotification()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun addMissCallNotification() {
        if (isFavorite() && isTimeOutToPickCall) {
            NotificationUtil(this).addMissCallPPNotification(getCallerUID())
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        joshAudioManager?.quitEverything()
        isEngineInitialized = false
        isTimeOutToPickCall = false
        switchChannel = false
        isCallerJoined = false
        callStartTime = 0L
        retryInitLibrary = 0
        userDetailMap = null
        stopForeground(true)
        notificationState = NotificationState.NOT_VISIBLE
        Timber.tag(TAG).e("onTaskRemoved")
        stopSelf()
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).e("OnTaskRemoved")
    }

    override fun onDestroy() {
        RtcEngine.destroy()
        stopRing()
        userDetailMap = null
        isEngineInitialized = false
        joshAudioManager?.quitEverything()
        AppObjectController.mRtcEngine = null
        handlerThread?.quitSafely()
        isTimeOutToPickCall = false
        isCallerJoined = false
        callStartTime = 0L
        retryInitLibrary = 0
        isCallOnGoing.postValue(false)
        switchChannel = false
        TelephonyUtil.getManager(this)
            .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_NONE)
        pstnCallState = CallState.CALL_STATE_IDLE
        Timber.tag(TAG).e("onDestroy")
        // removeSensor()
        executor.shutdown()
        stopForeground(true)
        notificationState = NotificationState.NOT_VISIBLE
        stopSelf()
        super.onDestroy()
    }

    private fun addNotification(action: String, data: HashMap<String, String?>?) {
        // mNotificationManager?.cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (action) {
                IncomingCall().action, FavoriteIncomingCall().action -> {
                    if (isOnPstnCall.not()) {
                        showNotification(
                            incomingCallNotification(data),
                            INCOMING_CALL_NOTIFICATION_ID
                        )
                        startRingtoneAndVibration()
                    }
                }
                CallConnect().action -> {
                    removeIncomingNotification()
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
        Timber.tag(TAG).e("showNotification")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(notificationId, notification)
        }
        notificationState = NotificationState.VISIBLE
    }

    private fun canHeadsUpNotification(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) { //  if (Build.VERSION.SDK_INT >= 29 && JoshApplication.isAppVisible.not()) {
            return true
        }
        return false
    }

    private fun incomingCallNotification(incomingData: HashMap<String, String?>?): Notification {
        Timber.tag(TAG).e("incomingCallNotification   ")
        val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            uniqueInt, getWebRtcActivityIntent(CallType.INCOMING),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val importance = if (canHeadsUpNotification()) IMPORTANCE_HIGH else IMPORTANCE_LOW

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
            var createChannel = false
            if (canHeadsUpNotification()) {
                val oldChannel =
                    mNotificationManager?.getNotificationChannel("incoming_calls$chanIndex")
                if (oldChannel != null) {
                    mNotificationManager?.deleteNotificationChannel(oldChannel.id)
                }
                val existingChannel =
                    mNotificationManager?.getNotificationChannel("incoming_calls2$chanIndex")
                if (existingChannel != null) {
                    mNotificationManager?.deleteNotificationChannel("incoming_calls2$chanIndex")
                    chanIndex++
                    PrefManager.put("calls_notification_channel", chanIndex)
                }
                createChannel = true
            } else {
                if (chanIndex == 0) {
                    createChannel = true
                }
            }
            val chan = NotificationChannel(
                "incoming_calls2$chanIndex", name,
                importance
            ).apply {
                description = "Notifications for voice calling"
            }
            chan.setSound(null, null)
            chan.enableVibration(false)
            chan.enableLights(false)
            chan.setBypassDnd(true)
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            if (createChannel) {
                try {
                    mNotificationManager?.createNotificationChannel(chan)
                } catch (e: Throwable) {
                    e.printStackTrace()
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
            Intent(this, WebRtcService::class.java)
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
            pendingIntent,
            true
        )
        val avatar: Bitmap? = getIncomingCallAvatar(isFavorite = isFavorite())
        val customView = getRemoteViews(isFavorite = isFavorite())

        customView.setImageViewBitmap(R.id.photo, avatar)
        customView.setOnClickPendingIntent(R.id.answer_btn, answerPendingIntent)
        customView.setOnClickPendingIntent(R.id.decline_btn, declinePendingIntent)
        builder.setLargeIcon(avatar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = importance
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVibrate(LongArray(0))
            builder.setCategory(Notification.CATEGORY_CALL)
        }
        if (canHeadsUpNotification()) {
            builder.setCustomHeadsUpContentView(customView)
            builder.setCustomBigContentView(customView)
            builder.setCustomContentView(customView)
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
        return if (getCallerUrl().isNullOrBlank()) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                R.layout.favorite_call_notification_patch
            } else {
                R.layout.favorite_call_notification
            }
        } else {
            R.layout.call_notification
        }
        val customView = RemoteViews(packageName, layout)
        customView.setTextViewText(
            R.id.name,
            if (isFavorite) {
                getString(R.string.favorite_p2p_title)
            } else {
                getString(R.string.p2p_title)
            }
        )
        customView.setTextViewText(
            R.id.title,
            if (isFavorite) {
                getCallerName()
            } else {
                getString(R.string.p2p_subtitle)
            }
        )

        customView.setTextViewText(
            R.id.answer_text,
            getActionText(
                R.string.answer,
                R.color.action_color
            )
        )
        customView.setTextViewText(
            R.id.decline_text,
            getActionText(
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
        val intent = getWebRtcActivityIntent(CallType.INCOMING).apply {
            putExtra(IS_CALL_CONNECTED, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            uniqueInt, intent,
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

        if (title != DEFAULT_NOTIFICATION_TITLE)
            lNotificationBuilder.setProgress(0, 0, true)

        lNotificationBuilder.priority = NotificationCompat.PRIORITY_MAX
        return lNotificationBuilder.build()
    }

    private fun getWebRtcActivityIntent(callType: CallType): Intent {
        return Intent(
            this,
            WebRtcActivity::class.java
        ).apply {
            putExtra(CALL_TYPE, callType)
            callData?.apply {
                if (isFavorite()) {
                    put(RTC_IS_FAVORITE, "true")
                }
            }
            putExtra(CALL_USER_OBJ, callData)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
                if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
                    data["is_demo"] =
                        PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false).toString()
                }
                val resp = AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
                if (resp.code() == 500) {
                    callCallback?.get()?.onNoUserFound()
                    return@launch
                }
                if (CallAction.ACCEPT == callAction) {
                    callCallback?.get()?.onServerConnect()
                    return@launch
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
data class UserJoined(val action: String = "calling.action.resume_call") : WebRtcCalling()

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

enum class VoipButtonState {
    SPEAKER, BLUETOOTH, DEFAULT, NONE
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
    fun onSpeakerOff() {}
    fun onBluetoothStateChanged(isOn: Boolean) {}
}

enum class NotificationState {
    VISIBLE, NOT_VISIBLE
}

/*enum class ServiceNotificationState {
    NONE, ACTION, INCOMING, CONNECTED
}*/
