@file:Suppress("UNCHECKED_CAST")

package com.joshtalks.joshskills.ui.voip

import android.annotation.SuppressLint
import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.*
import android.net.Uri
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
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.ACTION_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CALL_NOTIFICATION_CHANNEL
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.extra.FullScreenActivity
import com.joshtalks.joshskills.ui.voip.util.TelephonyUtil
import com.joshtalks.joshskills.ui.voip.util.WebRtcAudioManager
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.min


const val RTC_TOKEN_KEY = "token"
const val RTC_CHANNEL_KEY = "channel_name"
const val RTC_UID_KEY = "uid"
const val RTC_CALLER_UID_KEY = "caller_uid"

class WebRtcService : Service(), SensorEventListener {

    private var mNotificationManager: NotificationManager? = null
    private val mBinder: IBinder = MyBinder()
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")
    private val hangUpRtcOnDeviceCallAnswered: PhoneStateListener =
        HangUpRtcOnPstnCallAnsweredListener()

    private var compositeDisposable = CompositeDisposable()
    private var ringtonePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var callStartTime: Long = 0
    private var proximityWakelock: PowerManager.WakeLock? = null
    private var cpuWakelock: PowerManager.WakeLock? = null
    private var isProximityNear = false
    private var isRemoteUserAudioConnected = true
    private var joshAudioManager: WebRtcAudioManager? = null
    private var callForceDisconnect = false
    private var mHandler: Handler? = null
    private var handlerThread: HandlerThread? = null

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
        private var isMicEnable = true

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
        var isCallerJoin: Boolean = false

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
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun startOnNotificationIncomingCall(data: HashMap<String, String>) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = NotificationIncomingCall().action
                putExtra(CALL_USER_OBJ, data)
                putExtra(CALL_TYPE, CallType.INCOMING)
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun disconnectCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallDisconnect().action
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun disconnectCallFromCallie() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallStop().action
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun rejectCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallReject().action
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
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
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun forceDisconnect() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallForceDisconnect().action
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun noUserFoundCallDisconnect() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = NoUserFound().action
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun holdCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = HoldCall().action
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

        fun resumeCall() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = ResumeCall().action
            }
            if (JoshApplication.isAppVisible) {
                AppObjectController.joshApplication.startService(serviceIntent)
            } else {
                ContextCompat.startForegroundService(
                    AppObjectController.joshApplication,
                    serviceIntent
                )
            }
        }

    }

    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {

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
            if (callCallback?.get() != null) {
                callCallback?.get()?.onDisconnect(callId, callData?.let { getChannelName(it) })
            } else {
                RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
            }
            disconnectService()
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Timber.tag(TAG).e("onJoinChannelSuccess=  $channel = $uid   ")
            super.onJoinChannelSuccess(channel, uid, elapsed)
            compositeDisposable.clear()
            isCallWasOnGoing = true
            callData?.let {
                try {
                    val id = getUID(it)
                    if (callType == CallType.INCOMING && id == uid) {
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
            isCallerJoin = false
            if (switchChannel.not()) {
                callCallback?.get()?.onDisconnect(
                    callId,
                    callData?.let { getChannelName(it) },
                    TimeUnit.SECONDS.toMillis(stats.totalDuration.toLong())
                )
                switchChannel = false
            }
            callData = null

        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Timber.tag(TAG).e("onUserJoined=  $uid  $elapsed")
            super.onUserJoined(uid, elapsed)
            compositeDisposable.clear()
            isCallWasOnGoing = true
            isCallerJoin = true
            startCallTimer()
            addNotification(CallConnect().action, callData)
            callCallback?.get()?.onConnect(uid.toString())
            AppObjectController.uiHandler.postDelayed({
                callCallback?.get()?.onServerConnect()
            }, 500)
            addSensor()
            joshAudioManager?.startCommunication()
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Timber.tag(TAG).e("onUserOffline=  $uid  $reason")
            super.onUserOffline(uid, reason)
            isCallerJoin = false
            callData?.let {
                val id = getUID(it)
                Timber.tag(TAG).e("onUserOffline =  $id")
                if (id != uid && reason == Constants.USER_OFFLINE_QUIT) {
                    endCall(apiCall = true)
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

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            super.onConnectionStateChanged(state, reason)
            Timber.tag(TAG).e("onConnectionStateChanged " + state + "  " + reason)
        }

        override fun onNetworkTypeChanged(type: Int) {
            super.onNetworkTypeChanged(type)
            Timber.tag(TAG).e("onNetworkTypeChanged" + type)
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Timber.tag(TAG).e("onConnectionLost")
            lostNetwork()
        }

        private fun lostNetwork() {
            addTimerReconnect()
            callCallback?.get()?.onNetworkLost()
            joshAudioManager?.reconnectCommunication()
        }

        private fun gainNetwork() {
            compositeDisposable.clear()
            callCallback?.get()?.onNetworkReconnect()
            joshAudioManager?.reconnectCommunicationStop()
        }

    }

    inner class MyBinder : Binder() {
        fun getService(): WebRtcService {
            return this@WebRtcService
        }
    }/*
    handler.post(new Runnable(){…});*/

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
                    mHandler?.sendMessage(message)
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

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).e("onCreate")
        phoneCallState = CallState.CALL_STATE_IDLE
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        handlerThread = CustomHandlerThread("WebrtcThread")
        handlerThread?.start()
        if (handlerThread != null) {
            mHandler = Handler(handlerThread!!.looper)
        }

        executor.execute {
            try {
                TelephonyUtil.getManager(this)
                    .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_CALL_STATE)
                joshAudioManager = WebRtcAudioManager(this)
                cpuWakelock = (getSystemService(POWER_SERVICE) as PowerManager?)?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "joshtalsk"
                )
                cpuWakelock?.acquire(60 * 1000L /*1 minutes*/)
            } catch (ex: Exception) {
                ex.printStackTrace()
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
            mRtcEngine?.apply {
                if (BuildConfig.DEBUG) {
                    setParameters("{\"rtc.log_filter\": 65535}")
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

                // Configuration for the publisher. When the network condition is poor, send audio only.
                setLocalPublishFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)

                // Configuration for the subscriber. Try to receive low stream under poor network conditions. When the current network conditions are not sufficient for video streams, receive audio stream only.
                setRemoteSubscribeFallbackOption(Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY)

            }
            if (mRtcEngine != null) {
                callback.invoke()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand=  %s", intent?.action)
        executor.execute {
            intent?.action?.run {
                initEngine {
                    try {
                        callForceDisconnect = false
                        isRemoteUserAudioConnected = true

                        when {
                            this == InitLibrary().action -> {
                                Timber.tag(TAG).e("LibraryInit")
                            }
                            this == NotificationIncomingCall().action -> {
                                if (CallState.CALL_STATE_BUSY == phoneCallState || isCallWasOnGoing) {
                                    //phoneBusySoDisconnect(intent)
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
                                handleIncomingCall(data)
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
                                    callStatusNetworkApi(it, CallAction.DISCONNECT)
                                }
                                endCall()
                                isCallRecordOngoing = false
                            }
                            this == NoUserFound().action -> {
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
                                callCallback?.get()?.onHoldCall()
                            }
                            this == ResumeCall().action -> {
                                callCallback?.get()?.onUnHoldCall()
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
        callCallback?.get()
            ?.onDisconnect(callId, callData?.let { getChannelName(it) })
        if (callCallback?.get() != null) {
            RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
        }
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

    private fun handleIncomingCall(data: HashMap<String, String?>) {
        executeEvent(AnalyticsEvent.INIT_CALL.NAME)
        addNotification(NotificationIncomingCall().action, callData)
        startRingtoneAndVibration()
        //showIncomingCallScreen(data)
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

    private fun addTimerReconnect() {
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

    fun endCall(apiCall: Boolean = false) {
        Timber.tag(TAG).e("call_status%s", mRtcEngine?.connectionState)
        if (apiCall) {
            callData?.let {
                callStatusNetworkApi(it, CallAction.DISCONNECT)
            }
        }
        joshAudioManager?.endCommunication()
        if (isCallWasOnGoing) {
            isCallWasOnGoing = false
            mRtcEngine?.leaveChannel()
            disconnectService()
        } else {
            callStopWithoutIssue()
        }
    }


    private fun showIncomingCallScreen(
        data: HashMap<String, String?>,
        autoPickupCall: Boolean = false
    ) {
        val callActivityIntent =
            Intent(
                this, WebRtcActivity::class.java
            ).apply {
                putExtra(CALL_TYPE, CallType.INCOMING)
                putExtra(AUTO_PICKUP_CALL, autoPickupCall)
                putExtra(CALL_USER_OBJ, data)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(callActivityIntent)
    }

    fun startCallTimer() {
        callStartTime = SystemClock.elapsedRealtime()
    }

    fun getTimeOfTalk(): Long {
        return if (callStartTime == 0L) {
            0
        } else SystemClock.elapsedRealtime() - callStartTime
    }

    private fun isAppVisible(): Boolean {
        return if (JoshApplication.isAppVisible || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return true
        else
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && JoshApplication.isAppVisible.not()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun answerCall(data: HashMap<String, String?>) {
        executor.execute {
            try {
                stopRing()
                joinCall(data)
                //callCallback?.get()?.onConnect(EMPTY)
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
        isCallWasOnGoing = true
        mRtcEngine?.createDataStream(true, true)

    }

    @SuppressLint("InvalidWakeLockTag")
    private fun addSensor() {
        val sm: SensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val proximity: Sensor? = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        try {
            proximity?.run {
                proximityWakelock = (getSystemService(POWER_SERVICE) as PowerManager?)?.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "joshtalsk-prx"
                )
                sm.registerListener(
                    this@WebRtcService,
                    proximity,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        } catch (x: Exception) {
            x.printStackTrace()
        }
    }

    private fun removeSensor() {
        try {
            if (proximityWakelock != null && proximityWakelock!!.isHeld) {
                proximityWakelock?.release()
            }
            if (cpuWakelock != null && cpuWakelock!!.isHeld) {
                cpuWakelock?.release()
            }
            val sm = getSystemService(SENSOR_SERVICE) as SensorManager
            val proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            if (proximity != null) {
                sm.unregisterListener(this)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
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

    @SuppressLint("MissingPermission")
    private fun stopRing() {
        try {
            ringtonePlayer?.run {
                stop()
                release()
                ringtonePlayer = null
            }
            vibrator?.run {
                cancel()
                vibrator = null
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun muteCall() {
        mRtcEngine?.muteLocalAudioStream(true)
    }

    private fun unMuteCall() {
        mRtcEngine?.muteLocalAudioStream(false)
    }

    fun switchAudioSpeaker() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        isSpeakerEnable = !isSpeakerEnable
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        mRtcEngine?.setEnableSpeakerphone(isSpeakerEnable)
        mRtcEngine!!.setDefaultAudioRoutetoSpeakerphone(isSpeakerEnable)
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
        isRemoteUserAudioConnected = true
        joshAudioManager?.reconnectCommunicationStop()
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
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getSpeaker() = isSpeakerEnable

    fun getMic() = isMicEnable

    override fun onTaskRemoved(rootIntent: Intent?) {
        AppObjectController.mRtcEngine = null
        RtcEngine.destroy()
        handlerThread?.quitSafely()
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
        mNotificationManager?.cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (action) {
                NotificationIncomingCall().action -> {
                    showNotification(incomingCallNotification(data), INCOMING_CALL_NOTIFICATION_ID)
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
            //  .setSubText("Subtext")
            .setContentIntent(pendingIntent)
            .setChannelId(CALL_NOTIFICATION_CHANNEL)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )

        /*    val incomingSoundUri: Uri =
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + AppObjectController.joshApplication.packageName + "/" + R.raw.incoming)
    */
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
        val avatar: Bitmap? = getRandomName().textDrawableBitmap()
        val customView = RemoteViews(packageName, R.layout.call_notification)
        customView.setTextViewText(R.id.name, getString(R.string.p2p_title))
        customView.setTextViewText(R.id.title, getString(R.string.p2p_subtitle))

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
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_call_end,
                    getActionText(R.string.hang_up, R.color.error_color),
                    declineActionPendingIntent
                )
            )
            .setContentInfo("Outgoing call")
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

    private fun executeEvent(event: String) {
        executor.execute {
            AppAnalytics.create(event)
                .addUserDetails()
                .push()
        }
    }

    private fun callStatusNetworkApi(data: HashMap<String, String?>, callAction: CallAction) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val time = getTimeOfTalk()
                data["mentor_id"] = Mentor.getInstance().getId()
                data["call_response"] = callAction.action
                data["duration"] = TimeUnit.MILLISECONDS.toSeconds(time).toString()
                AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
                if (CallAction.ACCEPT == callAction) {
                    callCallback?.get()?.onServerConnect()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            if (am.isSpeakerphoneOn && am.isBluetoothScoOn) {
                return
            }
            val newIsNear: Boolean = event.values[0] < min(event.sensor.maximumRange, 3F)
            checkIsNear(newIsNear)
        }
    }

    private fun checkIsNear(newIsNear: Boolean) {
        if (newIsNear != isProximityNear) {
            isProximityNear = newIsNear
            try {
                if (isProximityNear) {
                    proximityWakelock?.acquire(30 * 60L * 60)
                } else {
                    proximityWakelock?.release(1)
                }
            } catch (x: Exception) {
                x.printStackTrace()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    @SuppressLint("MissingPermission")
    private fun startRingtoneAndVibration() {
        if (PrefManager.getBoolValue(CALL_RINGTONE_NOT_MUTE).not()) {
            return
        }

        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val needRing = am.ringerMode != AudioManager.RINGER_MODE_SILENT
        if (needRing) {
            val att: AudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            ringtonePlayer = MediaPlayer()
            ringtonePlayer?.setOnPreparedListener { mediaPlayer -> ringtonePlayer?.start() }
            ringtonePlayer?.isLooping = true
            ringtonePlayer?.setAudioAttributes(att)
            ringtonePlayer?.setAudioStreamType(AudioManager.STREAM_RING)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build()
                )
            } else {
                am.requestAudioFocus({ }, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN)
            }

            try {
                val notificationUri: String =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()
                ringtonePlayer?.setDataSource(this, Uri.parse(notificationUri))
                ringtonePlayer?.prepareAsync()
            } catch (e: java.lang.Exception) {
                if (ringtonePlayer != null) {
                    ringtonePlayer?.release()
                    ringtonePlayer = null
                }
            }

            if ((am.ringerMode == AudioManager.RINGER_MODE_VIBRATE || am.ringerMode == AudioManager.RINGER_MODE_NORMAL) || am.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(100, 250, 500, 750, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    vibrator?.vibrate(pattern, 0)
                }
            }
        }
    }

}

sealed class WebRtcCalling
data class NotificationIncomingCall(val action: String = "calling.action.notification_incoming_call") :
    WebRtcCalling()

data class InitLibrary(val action: String = "calling.action.initLibrary") : WebRtcCalling()
data class IncomingCall(val action: String = "calling.action.incoming_call") : WebRtcCalling()
data class CallConnect(val action: String = "calling.action.connect") : WebRtcCalling()
data class CallDisconnect(val action: String = "calling.action.disconnect") : WebRtcCalling()
data class CallReject(val action: String = "calling.action.callReject") : WebRtcCalling()

data class OutgoingCall(val action: String = "calling.action.outgoing_call") : WebRtcCalling()
data class LoginUser(val action: String = "calling.action.login") : WebRtcCalling()
data class LogoutUser(val action: String = "calling.action.logout") : WebRtcCalling()
data class CallStop(val action: String = "calling.action.stopcall") : WebRtcCalling()
data class CallForceConnect(val action: String = "calling.action.force_connect") : WebRtcCalling()
data class CallForceDisconnect(val action: String = "calling.action.force_disconnect") :
    WebRtcCalling()

data class NoUserFound(val action: String = "calling.action.no_user_found") :
    WebRtcCalling()

data class HoldCall(val action: String = "calling.action.hold_call") : WebRtcCalling()
data class ResumeCall(val action: String = "calling.action.resume_call") : WebRtcCalling()


enum class CallState(val state: Int) {
    CALL_STATE_CONNECTED(0), CALL_STATE_IDLE(1), CALL_STATE_BUSY(2),
    CONNECT(3), DISCONNECT(4), REJECT(5), ONHOLD(6), UNHOLD(7)
}

enum class CallAction(val action: String) {
    ACCEPT("ACCEPT"), DECLINE("DECLINE"), DISCONNECT("DISCONNECT"), TIMEOUT("TIMEOUT"),
    ONHOLD("ONHOLD"), RESUME("RESUME")
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

    fun onRemoteUserNetworkStateChanged(state: Boolean) {}

}