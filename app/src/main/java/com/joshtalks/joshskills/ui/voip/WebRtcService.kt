package com.joshtalks.joshskills.ui.voip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CallType
import com.joshtalks.joshskills.core.CountUpTimer
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.printAll
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_CHANNEL_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.EMPTY_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_CHANNEL_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.OUTGOING_CALL_CHANNEL_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.OUTGOING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.extra.FullScreenActivity
import com.joshtalks.joshskills.ui.voip.util.SoundPoolManager
import com.joshtalks.joshskills.ui.voip.util.TelephonyUtil
import io.agora.rtc.Constants
import io.agora.rtc.Constants.ERR_LEAVE_CHANNEL_REJECTED
import io.agora.rtc.Constants.USER_OFFLINE_QUIT
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.concurrent.ExecutorService


const val RTC_TOKEN_KEY = "token"
const val RTC_CHANNEL_KEY = "channel_name"
const val RTC_UID_KEY = "uid"

class WebRtcService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private val mBinder: IBinder = MyBinder()
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")
    private val hangUpRtcOnDeviceCallAnswered: PhoneStateListener =
        HangUpRtcOnPstnCallAnsweredListener()

    companion object {
        private val TAG = WebRtcService::class.java.simpleName
        private var phoneCallState = CallState.CALL_STATE_IDLE

        @Volatile
        private var isSpeakerEnable = false

        @Volatile
        private var isMicEnable = true

        @Volatile
        private var mRtcEngine: RtcEngine? = AppObjectController.getRtcEngine()

        @Volatile
        private var countUpTimer = CountUpTimer(false)

        @Volatile
        private var callData: HashMap<String, String?>? = null

        @Volatile
        private var callId: String? = null

        @Volatile
        private var callType: CallType = CallType.OUTGOING

        @Volatile
        private var outgoingCallData: HashMap<String, String?>? = null

        @Volatile
        private var incomingCallData: HashMap<String, String?>? = null


        @Volatile
        var isCallWasOnGoing: Boolean = false

        @Volatile
        var retryInitLibrary: Int = 0

        @Volatile
        private var callCallback: WeakReference<WebRtcCallback>? = null

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


    }

    fun addListener(callback: WebRtcCallback?) {
        callCallback = WeakReference(callback)
    }

    @Volatile
    private var eventListener = object : IRtcEngineEventHandler() {
        override fun onWarning(warn: Int) {
            super.onWarning(warn)
            Timber.tag(TAG).e("onWarning=  $warn")
        }

        override fun onError(errorCode: Int) {
            super.onError(errorCode)
            Timber.tag(TAG).e("onError=  $errorCode")
            if (ERR_LEAVE_CHANNEL_REJECTED == errorCode) {
                if (callCallback?.get() != null) {
                    callCallback?.get()?.onDisconnect(callId)
                } else {
                    RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                }
                onDisconnectAndRemove()
                isCallWasOnGoing = false
            }

        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            isCallWasOnGoing = true
            callData?.let {
                if (callType == CallType.INCOMING) {
                    callStatusNetworkApi(it, CallAction.ACCEPT)
                }
            }
            callId = mRtcEngine?.callId
            Timber.tag(TAG).e("onJoinChannelSuccess=  $channel = $uid   ")
        }

        override fun onRejoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onRejoinChannelSuccess(channel, uid, elapsed)
            Timber.tag(TAG).e("onRejoinChannelSuccess=  $channel = $uid   ")
        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
            Timber.tag(TAG).e("onLeaveChannel=  " + stats.totalDuration)
            callData?.printAll()
            callCallback?.get()?.onDisconnect(callId)
            isCallWasOnGoing = false
            callData = null
        }

        override fun onLocalUserRegistered(uid: Int, userAccount: String) {
            super.onLocalUserRegistered(uid, userAccount)
            Timber.tag(TAG).e("onLocalUserRegistered=  $uid  $userAccount")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Timber.tag(TAG).e("onUserJoined=  $uid  $elapsed")
            startCallTimer()
            callCallback?.get()?.onConnect()
            isCallWasOnGoing = true
        }

        //USER_OFFLINE_QUIT USER_OFFLINE_DROPPED   for outgoing call
        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            Timber.tag(TAG).e("onUserOffline=  $uid  $reason")
            callData?.let {
                val id = getUID(it)
                if (id != uid && reason == USER_OFFLINE_QUIT) {
                    endCall()
                }
            }
            isCallWasOnGoing = false
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            super.onConnectionStateChanged(state, reason)
            Timber.tag(TAG).e("onConnectionStateChanged=  $state  $reason")
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Timber.tag(TAG).e("onConnectionLost")

        }

        override fun onApiCallExecuted(error: Int, api: String, result: String) {
            super.onApiCallExecuted(error, api, result)
            Timber.tag(TAG).e("onApiCallExecuted=  $error  $api  $result")
        }

        override fun onRequestToken() {
            super.onRequestToken()
            Timber.tag(TAG).e("onRequestToken")
        }

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            Timber.tag(TAG).e("onAudioRouteChanged")
        }

        override fun onRtcStats(stats: RtcStats) {
            super.onRtcStats(stats)
            Timber.tag(TAG).e("onRtcStats" + stats.users)
        }

        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            Timber.tag(TAG).e("onAudioMixingStateChanged")
        }

        override fun onLocalAudioStateChanged(state: Int, error: Int) {
            super.onLocalAudioStateChanged(state, error)
            Timber.tag(TAG).e("onLocalAudioStateChanged")
        }

        override fun onRtmpStreamingStateChanged(url: String, state: Int, errCode: Int) {
            super.onRtmpStreamingStateChanged(url, state, errCode)
            Timber.tag(TAG).e("onRtmpStreamingStateChanged")
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
            phoneCallState = if (state == TelephonyManager.CALL_STATE_IDLE) {
                CallState.CALL_STATE_IDLE
            } else {
                CallState.CALL_STATE_BUSY
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
        mRtcEngine = AppObjectController.getRtcEngine()
        mRtcEngine?.removeHandler(eventListener)
        mRtcEngine?.addHandler(eventListener)
        if (mRtcEngine != null) {
            callback.invoke()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand=  %s", intent?.action)
        executor.execute {
            intent?.action?.run {
                addNotification(this)
                initEngine {
                    try {
                        when {
                            this == NotificationIncomingCall().action -> {
                                val data =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                data.let {
                                    incomingCallData = it
                                    callData = it
                                }
                                callType = CallType.INCOMING
                                handleIncomingCall(data)
                            }
                            this == OutgoingCall().action -> {
                                val data: HashMap<String, String?> =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                data.let {
                                    outgoingCallData = it
                                    callData = it
                                }
                                callType = CallType.OUTGOING
                                joinCall(data)
                            }
                            this == CallConnect().action -> {
                                mNotificationManager?.cancel(INCOMING_CALL_NOTIFICATION_ID)
                                mNotificationManager?.cancel(OUTGOING_CALL_NOTIFICATION_ID)
                                val incomingData: HashMap<String, String>? =
                                    intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String>?
                                val callActivityIntent =
                                    Intent(
                                        AppObjectController.joshApplication,
                                        WebRtcActivity::class.java
                                    ).apply {
                                        putExtra(CALL_TYPE, CallType.INCOMING)
                                        putExtra(AUTO_PICKUP_CALL, true)
                                        putExtra(CALL_USER_OBJ, incomingData)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                startActivity(callActivityIntent)
                            }
                            this == CallReject().action -> {
                                callData?.let {
                                    callStatusNetworkApi(it, CallAction.DECLINE)
                                    rejectCall()
                                }
                            }
                            this == CallDisconnect().action -> {
                                callData?.let {
                                    callStatusNetworkApi(it, CallAction.DISCONNECT)
                                }
                                endCall()
                            }
                            this == CallStop().action -> {
                                endCall()
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

    private fun addNotification(action: String) {
        when (action) {
            NotificationIncomingCall().action -> {

            }
            OutgoingCall().action -> {

            }
            CallConnect().action -> {

            }
            CallDisconnect().action -> {

            }
        }

    }

    private fun handleIncomingCall(data: HashMap<String, String?>) {
        if (CallState.CALL_STATE_BUSY == phoneCallState || isCallWasOnGoing) {
            rejectCall()
            return
        }
        startRing()
        executeEvent(AnalyticsEvent.INIT_CALL.NAME)
        showIncomingCallScreen(data)
    }

    private fun rejectCall() {
        callCallback?.get()?.onCallReject(callId)
        onDisconnectAndRemove()
    }

    fun endCall() {
        if (isCallWasOnGoing) {
            mRtcEngine?.leaveChannel()
        }
        Log.e("call_status", "" + mRtcEngine?.connectionState)
        onDisconnectAndRemove()
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
            countUpTimer.lap()
            countUpTimer.resume()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun stopTimer() {
        countUpTimer.pause()
    }

    fun getTimeOfTalk(): Int {
        return countUpTimer.time
    }


    private fun isAppVisible(): Boolean {
        return if (JoshApplication.isAppVisible || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            true
        else
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && JoshApplication.isAppVisible.not()
    }

    private fun processIncomingCall(incomingData: HashMap<String, String>?) {
        val callActivityIntent =
            Intent(this, WebRtcActivity::class.java).apply {
                putExtra(IS_INCOMING_CALL, true)
                putExtra(CALL_TYPE, CallType.INCOMING)
                putExtra(CALL_USER_OBJ, incomingData)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (isAppVisible()) {
            startActivities(arrayOf(callActivityIntent))
        }
        mNotificationManager?.cancel(EMPTY_NOTIFICATION_ID)
    }


    private fun showNotificationOnIncomingCall(incomingData: HashMap<String, String>?) {
        val notification = incomingCallNotification(incomingData)
        startForeground(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    private fun showNotificationOnOutgoingCall(extraHeaders: HashMap<String, String>?) {
        val notification = outgoingCallNotification(extraHeaders)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                OUTGOING_CALL_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(OUTGOING_CALL_NOTIFICATION_ID, notification)
        }
    }

    private fun showNotificationConnectedCall(
        header: HashMap<String, String>?,
        callType: CallType
    ) {
        val notification = callConnectNotification(header, callType)
        startForeground(CONNECTED_CALL_NOTIFICATION_ID, notification)
    }


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun answerCall(data: HashMap<String, String?>) {
        executor.execute {
            try {
                stopRing()
                joinCall(data)
                startCallTimer()
                callCallback?.get()?.onConnect()
                executeEvent(AnalyticsEvent.USER_ANSWER_EVENT_P2P.NAME)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun joinCall(data: HashMap<String, String?>) {
        executor.execute {
            mRtcEngine?.disableVideo()
            mRtcEngine?.enableAudio()
            mRtcEngine?.enableAudioVolumeIndication(1000, 3, true)
            mRtcEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                Constants.AUDIO_SCENARIO_EDUCATION
            )
            //  mRtcEngine?.removeHandler(eventListener)
            // mRtcEngine?.addHandler(eventListener)
            mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            mRtcEngine?.adjustRecordingSignalVolume(400)
            mRtcEngine?.adjustPlaybackSignalVolume(100)

            data.printAll()
            val statusCode = mRtcEngine?.joinChannel(
                getToken(data),
                getChannelName(data), "test",
                getUID(data)
            ) ?: -3
            Timber.tag(TAG).e("ha join$statusCode")

            if (statusCode < 0) {
                if (retryInitLibrary == 3) {
                    callStatusNetworkApi(data, CallAction.DECLINE)
                    return@execute
                }
                retryInitLibrary++
                try {
                    Thread.sleep(250)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                joinCall(data)
            }
            isCallWasOnGoing = true
        }
        //mRtcEngine?.connectionState
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

    private fun onDisconnectAndRemove() {
        stopTimer()
        isSpeakerEnable = false
        isMicEnable = true
        stopRing()
        phoneCallState = CallState.CALL_STATE_IDLE
        removeNotifications()
        incomingCallData = null
        outgoingCallData = null
    }

    private fun removeNotifications() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)?.cancelAll()
        stopForeground(true)
    }

    fun getSpeaker() = isSpeakerEnable
    fun getMic() = isMicEnable


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).e("OnTaskRemoved")
        countUpTimer.reset()
        RtcEngine.destroy()
    }

    override fun onDestroy() {
        retryInitLibrary = 0
        isCallWasOnGoing = false
        TelephonyUtil.getManager(this)
            .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_NONE)
        phoneCallState = CallState.CALL_STATE_IDLE
        Timber.tag(TAG).e("onDestroy")
    }


    private fun loginUserService(): Notification {
        Timber.tag(TAG).e("forground Notification ")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip Login User"
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(INCOMING_CALL_CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val lNotificationBuilder = NotificationCompat.Builder(this, INCOMING_CALL_CHANNEL_ID)
            .setChannelId(INCOMING_CALL_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Syncing...")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setOngoing(false)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)


        return lNotificationBuilder.build()
    }


    private fun incomingCallNotification(incomingData: HashMap<String, String>?): Notification {
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
                INCOMING_CALL_CHANNEL_ID,
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

        val answerActionIntent =
            Intent(AppObjectController.joshApplication, WebRtcService::class.java)
                .apply {
                    action = CallConnect().action
                    putExtra(CALL_USER_OBJ, incomingData)
                }

        val answerActionPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, answerActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val lNotificationBuilder = NotificationCompat.Builder(this, INCOMING_CALL_CHANNEL_ID)
            .setChannelId(INCOMING_CALL_CHANNEL_ID)
            .setContentTitle(incomingData?.get("X-PH-CALLERNAME"))
            .setContentText("Incoming voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            .setOngoing(true)
            //    .setContentIntent(pendingIntent)
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

        if (isAppVisible()) {
            lNotificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
            lNotificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
            lNotificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        return lNotificationBuilder.build()
    }

    private fun outgoingCallNotification(extraHeaders: HashMap<String, String>?): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip Outgoing Call"
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(OUTGOING_CALL_CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }
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
        val lNotificationBuilder = NotificationCompat.Builder(this, OUTGOING_CALL_CHANNEL_ID)
            .setContentTitle(extraHeaders?.get("X-PH-CALLIENAME"))
            .setContentText("Outgoing voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimary
                )
            )
            //.setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_call_end,
                    getActionText(R.string.hang_up, R.color.error_color),
                    declineActionPendingIntent
                )
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        return lNotificationBuilder.build()
    }

    private fun callConnectNotification(
        header: HashMap<String, String>?,
        callType: CallType
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip call connect"
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CONNECTED_CALL_CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }
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
        val lNotificationBuilder = NotificationCompat.Builder(this, CONNECTED_CALL_CHANNEL_ID)
            .setChannelId(CONNECTED_CALL_CHANNEL_ID)
            .setContentTitle(getNameAfterConnectedCall(header, callType))
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

    private fun getNameAfterConnectedCall(
        header: HashMap<String, String>?,
        callType: CallType
    ): String? {
        if (CallType.INCOMING == callType) {
            return header?.get("X-PH-CALLERNAME")
        }
        return header?.get("X-PH-CALLIENAME")
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
                data["mentor_id"] = Mentor.getInstance().getId()
                data["call_response"] = callAction.action
                val response =
                    AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


}


sealed class WebRtcCalling
data class NotificationIncomingCall(val action: String = "calling.action.notification_incoming_call") :
    WebRtcCalling()

data class IncomingCall(val action: String = "calling.action.incoming_call") : WebRtcCalling()
data class CallConnect(val action: String = "calling.action.connect") : WebRtcCalling()
data class CallDisconnect(val action: String = "calling.action.disconnect") : WebRtcCalling()
data class CallReject(val action: String = "calling.action.callReject") : WebRtcCalling()

data class OutgoingCall(val action: String = "calling.action.outgoing_call") : WebRtcCalling()
data class LoginUser(val action: String = "calling.action.login") : WebRtcCalling()
data class LogoutUser(val action: String = "calling.action.logout") : WebRtcCalling()
data class CallStop(val action: String = "calling.action.stopcall") : WebRtcCalling()

enum class CallState {
    CALL_STATE_IDLE, CALL_STATE_BUSY, CONNECT, DISCONNECT, REJECT
}

enum class CallAction(val action: String) {
    ACCEPT("ACCEPT"), DECLINE("DECLINE"), DISCONNECT("DISCONNECT")
}

class NotificationId {
    companion object {
        val EMPTY_NOTIFICATION_ID = 200000
        val INCOMING_CALL_NOTIFICATION_ID = 200001
        val OUTGOING_CALL_NOTIFICATION_ID = 200002
        val CONNECTED_CALL_NOTIFICATION_ID = 200003
        val INCOMING_CALL_CHANNEL_ID = "incoming_call_channel_id"
        val OUTGOING_CALL_CHANNEL_ID = "outgoing_call_channel_id"
        val CONNECTED_CALL_CHANNEL_ID = "connected_call_channel_id"
    }
}

interface WebRtcCallback {
    fun onConnect()
    fun onDisconnect(callId: String?)
    fun onCallReject(id: String?)
    fun onSelfDisconnect(id: String?)
    fun onIncomingCallHangup(id: String?)
}
