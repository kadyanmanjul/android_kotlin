@file:Suppress("UNCHECKED_CAST")

package com.joshtalks.joshskills.ui.voip

import android.app.*
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import com.joshtalks.joshskills.ui.voip.util.SoundPoolManager
import com.joshtalks.joshskills.ui.voip.util.TelephonyUtil
import io.agora.rtc.Constants
import io.agora.rtc.Constants.CONNECTION_STATE_DISCONNECTED
import io.agora.rtc.Constants.USER_OFFLINE_QUIT
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit


const val RTC_TOKEN_KEY = "token"
const val RTC_CHANNEL_KEY = "channel_name"
const val RTC_UID_KEY = "uid"
const val RTC_CALLER_UID_KEY = "caller_uid"

class WebRtcService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private val mBinder: IBinder = MyBinder()
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")
    private val hangUpRtcOnDeviceCallAnswered: PhoneStateListener =
        HangUpRtcOnPstnCallAnsweredListener()
    private var countUpTimer = CountUpTimer(false)
    private var compositeDisposable = CompositeDisposable()

    companion object {
        private val TAG = WebRtcService::class.java.simpleName
        private var phoneCallState = CallState.CALL_STATE_IDLE

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
        var retryInitLibrary: Int = 0

        @JvmStatic
        @Volatile
        var isCallRecordOngoing: Boolean = false

        @JvmStatic
        @Volatile
        var switchChannel: Boolean = false

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


    }

    fun addListener(callback: WebRtcCallback?) {
        callCallback = WeakReference(callback)
    }


    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {
        override fun onWarning(warn: Int) {
            super.onWarning(warn)
            Timber.tag(TAG).e("onWarning=  $warn")
        }

        override fun onError(errorCode: Int) {
            super.onError(errorCode)
            Timber.tag(TAG).e("onError=  $errorCode")
            //  isCallWasOnGoing = false
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
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Timber.tag(TAG).e("onJoinChannelSuccess=  $channel = $uid   ")
            compositeDisposable.clear()
            isCallWasOnGoing = true
            callData?.let {
                try {
                    val id = getUID(it)
                    if (callType == CallType.INCOMING && id == uid) {
                        //       callCallback?.get()?.onConnect(uid.toString())
                        startCallTimer()
                        callStatusNetworkApi(it, CallAction.ACCEPT)
                        addNotification(CallConnect().action, callData)
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
            super.onLeaveChannel(stats)
            Timber.tag(TAG).e("onLeaveChannel=  %s", stats.totalDuration)
            isCallWasOnGoing = false
            isCallerJoin = false
            if (switchChannel.not()) {
                callCallback?.get()?.onDisconnect(callId, callData?.let { getChannelName(it) })
                switchChannel = false
            }
            callData = null
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Timber.tag(TAG).e("onUserJoined=  $uid  $elapsed")
            isCallWasOnGoing = true
            isCallerJoin = true
            startCallTimer()
            addNotification(CallConnect().action, callData)
            callCallback?.get()?.onConnect(uid.toString())
            AppObjectController.uiHandler.postDelayed({
                callCallback?.get()?.onServerConnect()
            }, 500)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            Timber.tag(TAG).e("onUserOffline=  $uid  $reason")
            isCallerJoin = false
            callData?.let {
                val id = getUID(it)
                Timber.tag(TAG).e("onUserOffline =  $id")
                if (id != uid && reason == USER_OFFLINE_QUIT) {
                    endCall()
                    isCallWasOnGoing = false
                }
            }
        }
    }

    inner class MyBinder : Binder() {
        fun getService(): WebRtcService {
            return this@WebRtcService
        }
    }


    private class HangUpRtcOnPstnCallAnsweredListener : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            Timber.tag(TAG).e("RTC=    %s", state)
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                phoneCallState = CallState.CALL_STATE_IDLE
            } else {
                phoneCallState = CallState.CALL_STATE_BUSY
                disconnectCall()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Timber.tag(TAG).e("onCreate")
            phoneCallState = CallState.CALL_STATE_IDLE
            mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            TelephonyUtil.getManager(this)
                .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun initEngine(callback: () -> Unit) {
        try {
            mRtcEngine = AppObjectController.getRtcEngine()
            if (eventListener != null) {
                mRtcEngine?.removeHandler(eventListener)
            }
            if (eventListener != null) {
                mRtcEngine?.addHandler(eventListener)
            }
            try {
                Thread.sleep(250)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            mRtcEngine?.apply {
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
                        when {
                            this == InitLibrary().action -> {
                                Timber.tag(TAG).e("LibraryInit")
                            }
                            this == NotificationIncomingCall().action -> {
                                if (CallState.CALL_STATE_BUSY == phoneCallState || isCallWasOnGoing) {
                                    phoneBusySoDisconnect(intent)
                                    return@initEngine
                                }
                                resetTimer()
                                val data =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                data.let {
                                    callData = it
                                }
                                callType = CallType.INCOMING
                                handleIncomingCall(data)
                            }
                            this == OutgoingCall().action -> {
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
                                resetTimer()
                                mRtcEngine?.leaveChannel()
                                callCallback?.get()?.onNoUserFound()
                                disconnectService()
                            }
                            this == CallStop().action -> {
                                addNotification(CallDisconnect().action, null)
                                callStopWithoutIssue()
                            }
                            this == CallForceDisconnect().action -> {
                                if (JoshApplication.isAppVisible.not()) {
                                    addNotification(CallDisconnect().action, null)
                                }
                                resetTimer()
                                endCall(apiCall = false)
                                RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                            }
                            this == CallForceConnect().action -> {
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
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun phoneBusySoDisconnect(intent: Intent) {
        (intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?)?.let {
            callStatusNetworkApi(it, CallAction.DECLINE)
        }
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
        stopTimer()
        startRing()
        executeEvent(AnalyticsEvent.INIT_CALL.NAME)
        showIncomingCallScreen(data)
        addNotification(NotificationIncomingCall().action, callData)
        addTimeObservable()
    }

    private fun addTimeObservable() {
        compositeDisposable.add(
            Completable.complete()
                .delay(10, TimeUnit.SECONDS)
                .doOnComplete {
                    if (isCallNotConnected()) {
                        WebRtcService.rejectCall()
                    }
                }
                .subscribe())
    }

    fun isCallNotConnected(): Boolean {
        return (mRtcEngine?.connectionState == CONNECTION_STATE_DISCONNECTED || isCallWasOnGoing.not())
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
                AppObjectController.joshApplication,
                WebRtcActivity::class.java
            ).apply {
                putExtra(CALL_TYPE, CallType.INCOMING)
                putExtra(AUTO_PICKUP_CALL, autoPickupCall)
                putExtra(CALL_USER_OBJ, data)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(callActivityIntent)
    }


    private fun startRing() {
        try {
            SoundPoolManager.getInstance(AppObjectController.joshApplication).playRinging()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun startCallTimer() {
        try {
            countUpTimer.reset()
            //countUpTimer.lap()
            countUpTimer.resume()
        } catch (ex: Exception) {
            //   ex.printStackTrace()
        }
    }

    private fun stopTimer() {
        countUpTimer.pause()
    }

    private fun resetTimer() {
        try {
            countUpTimer.reset()
        } catch (ex: Exception) {
        }
    }

    fun getTimeOfTalk(): Int {
        return countUpTimer.time
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
        resetTimer()
        if (callData == null) {
            callData = data
        }
        data.printAll()
        val statusCode = mRtcEngine?.joinChannel(
            getToken(data),
            getChannelName(data), "test",
            getUID(data)
        ) ?: -3
        Timber.tag(TAG).e("ha join$statusCode")

        if (statusCode < 0) {
            if (retryInitLibrary == 3) {
                WebRtcService.rejectCall()
                return
            }
            retryInitLibrary++
            try {
                Thread.sleep(250)
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


    private fun stopRing() {
        try {
            SoundPoolManager.getInstance(AppObjectController.joshApplication).stopRinging()
        } catch (ex: Exception) {
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
        //  AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        isCallerJoin = false
        eventListener = null
        stopRing()
        stopTimer()
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
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)?.cancelAll()
            stopForeground(true)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getSpeaker() = isSpeakerEnable
    fun getMic() = isMicEnable


    override fun onTaskRemoved(rootIntent: Intent?) {
        isCallRecordOngoing = false
        switchChannel = false
        isCallerJoin = false
        RtcEngine.destroy()
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).e("OnTaskRemoved")
    }

    override fun onDestroy() {
        RtcEngine.destroy()
        AppObjectController.mRtcEngine = null
        executor.shutdown()
        isCallerJoin = false
        countUpTimer.reset()
        isCallRecordOngoing = false
        retryInitLibrary = 0
        isCallWasOnGoing = false
        switchChannel = false
        TelephonyUtil.getManager(this)
            .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_NONE)
        phoneCallState = CallState.CALL_STATE_IDLE
        Timber.tag(TAG).e("onDestroy")
    }


    private fun addNotification(action: String, data: HashMap<String, String?>?) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)?.cancelAll()
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
        val incomingSoundUri: Uri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + AppObjectController.joshApplication.packageName + "/" + R.raw.incoming)

        val att: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip Incoming Call"
            val mChannel = NotificationChannel(
                CALL_NOTIFICATION_CHANNEL,
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            mChannel.enableLights(true)
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = (longArrayOf(0, 1000, 500, 1000))
            mChannel.setSound(incomingSoundUri, att)
            mNotificationManager?.createNotificationChannel(mChannel)
        }
        val fullScreenPendingIntent = FullScreenActivity.getPendingIntent(this, 22)

        val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            uniqueInt, getWebRtcActivityIntent(CallType.INCOMING),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val declineActionIntent =
            Intent(AppObjectController.joshApplication, WebRtcService::class.java)
        declineActionIntent.action = CallReject().action
        val declineActionPendingIntent: PendingIntent =
            PendingIntent.getService(
                this,
                0,
                declineActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val answerActionIntent =
            Intent(AppObjectController.joshApplication, WebRtcService::class.java)
                .apply {
                    action = CallConnect().action
                    putExtra(CALL_USER_OBJ, incomingData)
                }

        val answerActionPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, answerActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val lNotificationBuilder = NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL)
            .setChannelId(CALL_NOTIFICATION_CHANNEL)
            //.setContentTitle(incomingData?.get("X-PH-CALLERNAME"))
            .setContentText("Incoming voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_call_end,
                    getActionText(R.string.hang_up, R.color.error_color),
                    declineActionPendingIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_pick_call,
                    getActionText(R.string.answer, R.color.action_color),
                    answerActionPendingIntent
                )
            )
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setPriority(NotificationCompat.PRIORITY_MAX)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        lNotificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
        lNotificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
        lNotificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL)
        return lNotificationBuilder.build()
    }

    private fun callConnectedNotification(data: HashMap<String, String?>?): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip call connect"
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CALL_NOTIFICATION_CHANNEL, name, importance)
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
            val name: CharSequence = "Voip Call Status"
            val mChannel = NotificationChannel(
                CALL_NOTIFICATION_CHANNEL,
                name,
                NotificationManager.IMPORTANCE_MIN
            )
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
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setProgress(0, 0, true)
        lNotificationBuilder.priority = NotificationCompat.PRIORITY_MIN
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
                data["duration"] = TimeUnit.MILLISECONDS.toSeconds(time.toLong()).toString()
                AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
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


enum class CallState {
    CALL_STATE_IDLE, CALL_STATE_BUSY, CONNECT, DISCONNECT, REJECT
}

enum class CallAction(val action: String) {
    ACCEPT("ACCEPT"), DECLINE("DECLINE"), DISCONNECT("DISCONNECT"), TIMEOUT("TIMEOUT")
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
    fun onDisconnect(callId: String?, channelName: String?) {}
    fun onCallReject(callId: String?) {}
    fun switchChannel(data: HashMap<String, String?>) {}
    fun onNoUserFound() {}
    fun onServerConnect() {}

}