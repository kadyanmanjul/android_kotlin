package com.joshtalks.badebhaiya.liveroom.service

import android.annotation.SuppressLint
import android.app.*
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.liveroom.ConversationLiveRoomActivity
import com.joshtalks.badebhaiya.liveroom.service.NotificationId.Companion.INCOMING_CALL_NOTIFICATION_ID
import com.joshtalks.badebhaiya.liveroom.service.NotificationId.Companion.ROOM_CALL_NOTIFICATION_ID
import com.joshtalks.badebhaiya.liveroom.service.NotificationId.Companion.ROOM_NOTIFICATION_CHANNEL
import com.joshtalks.badebhaiya.liveroom.service.util.TelephonyUtil
import com.joshtalks.badebhaiya.liveroom.service.util.WebRtcAudioManager
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.User
import io.agora.rtc.Constants
import io.agora.rtc.Constants.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtc.models.ClientRoleOptions
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.*

const val ROOM_RTC_USER_UID_KEY = "room_user_uid"
const val ROOM_RTC_MODERATOR_UID_KEY = "room_moderator_uid"
const val ROOM_RTC_TOKEN_KEY = "room_rtc_token"
const val ROOM_RTC_CHANNEL_KEY = "room_channel_name"
const val ROOM_RTC_CHANNEL_TOPIC = "room_channel_topic"
const val ROOM_RTC_ROOM_ID = "room_id"
const val ROOM_RTC_ROOM_Q = "room_q_id"
const val ROOM_RTC_IS_MODERATOR = "room_is_moderator"

class ConvoWebRtcService : Service() {

    protected val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Conversation_Room Service")

    protected var joshAudioManager: WebRtcAudioManager? = null
    protected var mNotificationManager: NotificationManager? = null

    private val TAG = "ABCService"
    private val mBinder: IBinder = MyBinder()
    private val hangUpRtcOnDeviceCallAnswered: PhoneStateListener =
        HangUpRtcOnPstnCallAnsweredListener()

    var speakingUsersNewList = arrayListOf<Int>()
    var speakingUsersOldList = arrayListOf<Int>()
    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val repository by lazy {
        ConversationRoomRepository()
    }
    private var isEngineInitialized = false

    companion object {
        var isRoomEnded = false
        var conversationRoomTopicName: String? = ""
        var pstnCallState = CallState.CALL_STATE_IDLE
        var isOnPstnCall = false
        var isRoomCreatedByUser = false
        var agoraUid: Int? = null
        var moderatorUid: Int? = null
        var roomId: Int? = null
        var channelTopic: String? = null
        var roomQuestionId: Int? = null
        var conversationRoomChannelName: String? = null
        var conversationRoomToken: String? = null

        @JvmStatic
        private val callReconnectTime = 5000

        @Volatile
        @JvmStatic
        private var rtcEngine: RtcEngine? = null

        @Volatile
        var retryInitLibrary: Int = 0

        @Volatile
        private var conversationRoomCallback: WeakReference<ConversationRoomCallback>? = null


        fun conversationRoomJoin(
            token: String?,
            channelName: String?,
            uid: Int?,
            moderatorId: Int?,
            channelTopic: String?,
            roomId: Int?,
            roomQuestionId: Int?,
            isModerator: Boolean? = false
        ) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                ConvoWebRtcService::class.java
            ).apply {
                action = ConversationRoomJoin().action
                putExtra(ROOM_RTC_TOKEN_KEY, token)
                putExtra(ROOM_RTC_CHANNEL_KEY, channelName)
                putExtra(ROOM_RTC_USER_UID_KEY, uid)
                putExtra(ROOM_RTC_MODERATOR_UID_KEY, moderatorId)
                putExtra(ROOM_RTC_CHANNEL_TOPIC, channelTopic)
                putExtra(ROOM_RTC_ROOM_ID, roomId)
                putExtra(ROOM_RTC_ROOM_Q, roomQuestionId)
                putExtra(ROOM_RTC_IS_MODERATOR, isModerator)
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun initLibrary() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                ConvoWebRtcService::class.java
            ).apply {
                action = InitLibrary().action
            }
            serviceIntent.startServiceForWebrtc()
        }
    }

    @Volatile
    private var conversationRoomEventListener: IRtcEngineEventHandler? =
        object : IRtcEngineEventHandler() {

            override fun onUserJoined(uid: Int, elapsed: Int) {
                super.onUserJoined(uid, elapsed)
                Log.d(TAG, "IRtcEngineEventHandler onUserJoined() called with: uid = $uid, elapsed = $elapsed")
            }

            override fun onWarning(warn: Int) {
                super.onWarning(warn)
                Log.d(TAG, "onWarning() called with: warn = $warn")
            }

            override fun onError(err: Int) {
                super.onError(err)
                Log.d(TAG, "IRtcEngineEventHandler onError() called with: err = $err")
            }

            override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                super.onJoinChannelSuccess(channel, uid, elapsed)
                Log.d(TAG, "IRtcEngineEventHandler onJoinChannelSuccess uuid $uid  moderatorUid $moderatorUid")

            }

            override fun onLeaveChannel(stats: RtcStats) {
                super.onLeaveChannel(stats)
                Log.d(TAG, "IRtcEngineEventHandler onLeaveChannel() called with: stats = $stats")
            }

            override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                super.onRejoinChannelSuccess(channel, uid, elapsed)
                Log.d(TAG, "IRtcEngineEventHandler RejoinChannelSuccess $uid")

            }

            override fun onUserOffline(uid: Int, reason: Int) {
                super.onUserOffline(uid, reason)
                Log.d(TAG, "IRtcEngineEventHandler onUserOffline() called with: uid = $uid, reason = $reason")
                val isUserLeave = reason == USER_OFFLINE_QUIT
                if (!isRoomCreatedByUser && isUserLeave) {
                    conversationRoomCallback?.get()?.onUserOffline(uid)
                }
                if (isRoomCreatedByUser) {
                    if (isUserLeave) {
                        Log.d(
                            TAG,
                            "IRtcEngineEventHandler isRoomCreatedByUser $isRoomCreatedByUser service OnUserOffline remove user by moderator $moderatorUid"
                        )
                    }
                } else {
                    if (uid == moderatorUid && isUserLeave) {
                        Log.d(
                            TAG,
                            "IRtcEngineEventHandler isRoomCreatedByUser $isRoomCreatedByUser  service OnUserOffline for moderator call $moderatorUid $reason"
                        )
                    }
                }


            }

            override fun onAudioVolumeIndication(
                speakers: Array<out AudioVolumeInfo>?,
                totalVolume: Int
            ) {
                super.onAudioVolumeIndication(speakers, totalVolume)

                conversationRoomCallback?.get()?.onAudioVolumeIndication(speakers, totalVolume)
            }

            override fun onClientRoleChanged(oldRole: Int, newRole: Int) {
                super.onClientRoleChanged(oldRole, newRole)
                Log.d(
                    TAG,
                    "onClientRoleChanged() called with: oldRole = $oldRole, newRole = $newRole"
                )
            }


        }


    fun endRoom(roomId: Int?, conversationQuestionId: Int? = null) {
        Log.d(
            TAG,
            "endRoom() service called with: roomId = $roomId, conversationQuestionId = $conversationQuestionId"
        )
        CoroutineScope(Dispatchers.IO).launch {
            removeNotifications()
            //removeConversationNotifications()
            try {
                if (isRoomEnded.not()) {
                    var qId: Int? = null
                    if (conversationQuestionId != null && (conversationQuestionId != 0 || conversationQuestionId != -1)) {
                        qId = conversationQuestionId
                    }
                    val response = repository.endRoom(
                        ConversationRoomRequest(
                            userId = User.getInstance().userId,
                            roomId = roomId
                        )
                    )
                    //WebRtcService.initLibrary()
                    Log.d(TAG, "end room api call ${response.code()}")
                    if (response.isSuccessful) {
                        isRoomEnded = false
                        conversationRoomChannelName = null
                        rtcEngine?.leaveChannel()
                        //joshAudioManager?.endCommunication()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun leaveRoom(roomId: Int?, conversationQuestionId: Int? = null) {
        if (roomId!=null) {
            CoroutineScope(Dispatchers.IO).launch {
                if (isRoomEnded.not()) {
                    removeNotifications()
                    try {
                        var qId: Int? = null
                        if (conversationQuestionId != null && (conversationQuestionId != 0 || conversationQuestionId != -1)) {
                            qId = conversationQuestionId
                        }
                        val response = repository.leaveRoom(
                            ConversationRoomRequest(
                                userId = User.getInstance().userId,
                                roomId = roomId
                            )
                        )
                        //WebRtcService.initLibrary()
                        Log.d(TAG, "leave room api call")
                        if (response.isSuccessful) {
                            isRoomEnded = false
                            conversationRoomChannelName = null
                            rtcEngine?.leaveChannel()
                            //joshAudioManager?.endCommunication()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

    fun endService() {
        CoroutineScope(Dispatchers.IO).launch {
            removeNotifications()
            conversationRoomChannelName = null
            rtcEngine?.leaveChannel()
        }
    }

    inner class HangUpRtcOnPstnCallAnsweredListener : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            Log.d(
                TAG,
                "onCallStateChanged() called with: RTC= = $state, phoneNumber = $phoneNumber"
            )
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    //TODO recheck this code

                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (isRoomCreatedByUser) {
                        Log.d(
                            TAG,
                            "CALL_STATE_OFFHOOK  called with: state = $state, phoneNumber = $phoneNumber"
                        )
                        endRoom(roomId, roomQuestionId)
                    } else {
                        leaveRoom(roomId, roomQuestionId)
                    }

                }
                TelephonyManager.CALL_STATE_RINGING -> {

                }
                else -> {
                    isOnPstnCall = true
                    pstnCallState = CallState.CALL_STATE_BUSY
                }
            }
        }
    }

    inner class MyBinder : Binder() {
        fun getService(): ConvoWebRtcService {
            return this@ConvoWebRtcService
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

        try {
            joshAudioManager = WebRtcAudioManager(this)

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        Log.d(TAG, "onCreate() called")
        pstnCallState = CallState.CALL_STATE_IDLE
        /*handlerThread.start()
        mHandler = Handler(handlerThread.looper)*/
        CoroutineScope(Dispatchers.IO).launch {
            TelephonyUtil.getManager(this@ConvoWebRtcService)
                .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_CALL_STATE)
        }

    }

    private fun initEngine(callback: () -> Unit) {
        try {
            rtcEngine = AppObjectController.initRtcEngine(AppObjectController.joshApplication)
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            Log.d(
                TAG,
                "initEngine called room id : $roomId isRoomCreatedByUser $isRoomCreatedByUser agoraUid:" +
                        " $agoraUid moderatorUid: $moderatorUid  isEngineInitialized: ${rtcEngine != null} listnerAdded: ${conversationRoomEventListener != null} "
            )
            if (conversationRoomEventListener != null) {
                rtcEngine?.removeHandler(conversationRoomEventListener)
            }
            if (conversationRoomEventListener != null) {
                rtcEngine?.addHandler(conversationRoomEventListener)
            }

            if (isEngineInitialized) {
                callback.invoke()
                return
            }

            rtcEngine?.apply {
                if (BuildConfig.DEBUG) {
                    //setParameters("{\"rtc.log_filter\": 65535}")
                    //setParameters("{\"che.audio.start_debug_recording\":\"all\"}")
                }
                setParameters("{\"rtc.peer.offline_period\":${callReconnectTime}}")
                setParameters("{\"che.audio.keep.audiosession\":true}")

                val statusCode = setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                Log.d(TAG, "initEngine() called after setting Profile ${statusCode}")
                if (statusCode<0){
                    if (retryInitLibrary == 3) {
                        if (isRoomCreatedByUser) {
                            endRoom(roomId, roomQuestionId)
                        } else {
                            leaveRoom(roomId, roomQuestionId)
                        }
                    }
                    retryInitLibrary++
                    initEngine(callback)

                }

                enableAudioVolumeIndication(1800, 3, true)
                setAudioProfile(
                    AUDIO_PROFILE_SPEECH_STANDARD,
                    AUDIO_SCENARIO_GAME_STREAMING
                )
                setDefaultAudioRoutetoSpeakerphone(true)
                val option = ChannelMediaOptions()
                option.autoSubscribeAudio = true

            }
            if (rtcEngine != null) {
                isEngineInitialized = true
                callback.invoke()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "onStartCommand() called with: intent = $intent, flags = $flags, startId = $startId"
        )
        removeNotifications()
        executor.execute {
            intent?.action?.run {
                setData(intent)
                initEngine {
                    try {
                        when {
                            this == InitLibrary().action -> {
                                Log.d(TAG, "LibraryInit() called")
                                showNotification(
                                    conversationRoomNotification(InitLibrary().action),
                                    ROOM_CALL_NOTIFICATION_ID
                                )
                            }
                            this == ConversationRoomJoin().action -> {
                                showNotification(
                                    conversationRoomNotification(ConversationRoomJoin().action),
                                    ROOM_CALL_NOTIFICATION_ID
                                )
                                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                                rtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)
                                audioManager.isSpeakerphoneOn = true
                                Log.d(TAG, "joinChannel")
                                val option = ChannelMediaOptions().apply {
                                    autoSubscribeAudio = true
                                    publishLocalAudio = isRoomCreatedByUser
                                    publishLocalVideo = false
                                    autoSubscribeVideo = false
                                }
                                val statusCode = agoraUid?.let {
                                    joinChannel(conversationRoomToken,conversationRoomChannelName,"test",it,option)
                                } ?: -3
                                Log.d(TAG, "join channel result retry ${statusCode}")

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

    fun joinChannel(
        conversationRoomToken: String?,
        conversationRoomChannelName: String?,
        s: String,
        i: Int,
        option: ChannelMediaOptions
    ): Int {
        rtcEngine?.leaveChannel()
        val statusCode = rtcEngine?.joinChannel(
            conversationRoomToken,
            conversationRoomChannelName, s,
            i, option
        ) ?: -3

        Log.d(
            TAG,
            "joinChannel() statusCode ${statusCode}"
        )
        if (statusCode < 0) {
            if (retryInitLibrary == 3) {
                if (isRoomCreatedByUser) {
                    endRoom(roomId, roomQuestionId)
                } else {
                    leaveRoom(roomId, roomQuestionId)
                }
                return retryInitLibrary
            }
            retryInitLibrary++
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            initEngine {
                joinChannel(conversationRoomToken, conversationRoomChannelName, s, i, option)
            }
        }
        if (isRoomCreatedByUser) {
            val code = rtcEngine?.setClientRole(CLIENT_ROLE_BROADCASTER)?:-3
            Log.d(TAG, "Broadcaster role setClient ${code}")

        } else {
            val clientRoleOptions = ClientRoleOptions()
            clientRoleOptions.audienceLatencyLevel =
                if (false) AUDIENCE_LATENCY_LEVEL_ULTRA_LOW_LATENCY else AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
            val code = rtcEngine?.setClientRole(CLIENT_ROLE_AUDIENCE, clientRoleOptions) ?:-3
            Log.d(TAG, "Audience role setClient ${code}")
        }
        rtcEngine?.setEnableSpeakerphone(true)
        return retryInitLibrary
    }

    private fun setData(intent: Intent) {
        agoraUid = intent.getIntExtra(ROOM_RTC_USER_UID_KEY, 0)
        conversationRoomToken = intent.getStringExtra(ROOM_RTC_TOKEN_KEY)
        conversationRoomChannelName = intent.getStringExtra(ROOM_RTC_CHANNEL_KEY)
        moderatorUid = intent.getIntExtra(ROOM_RTC_MODERATOR_UID_KEY, 0)
        channelTopic = intent.getStringExtra(ROOM_RTC_CHANNEL_TOPIC)
        roomId = intent.getIntExtra(ROOM_RTC_ROOM_ID, 0)
        roomQuestionId = intent.getIntExtra(ROOM_RTC_ROOM_Q, 0)
        isRoomCreatedByUser = intent.getBooleanExtra(ROOM_RTC_IS_MODERATOR, false)
        Log.d(
            TAG,
            "setData() called with: agoraUid = $agoraUid token ${conversationRoomToken} name ${conversationRoomChannelName} mUid ${moderatorUid} Topic ${channelTopic} roomId ${roomId} roomQ ${roomQuestionId}  ${isRoomCreatedByUser} "
        )
    }

    fun addListener(callback: ConversationRoomCallback) {
        conversationRoomCallback = WeakReference(callback)
    }

    fun removeIncomingNotification() {
        mNotificationManager?.cancel(ROOM_CALL_NOTIFICATION_ID)
        mNotificationManager?.cancel(INCOMING_CALL_NOTIFICATION_ID)
    }

    fun muteCall() {
        val log = rtcEngine?.muteLocalAudioStream(true)
        Log.d(TAG, "muteCall() called ${log}")
    }

    fun unMuteCall() {
        val log = rtcEngine?.muteLocalAudioStream(false)
        Log.d(TAG, "unMuteCall() called ${log}")

    }

    fun setClientRole(role: Int) {
        if (role == CLIENT_ROLE_AUDIENCE) {
            val clientRoleOptions = ClientRoleOptions()
            clientRoleOptions.audienceLatencyLevel =
                if (false) AUDIENCE_LATENCY_LEVEL_ULTRA_LOW_LATENCY else AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
            val client = rtcEngine?.setClientRole(role,clientRoleOptions)?:-3
            if (client<0){
                muteCall()
            }
            Log.d(TAG, "setClientRole() called with: CLIENT_ROLE_AUDIENCE setclient = $client")

        } else {
            val client = rtcEngine?.setClientRole(role)?:-3
            Log.d(TAG, "setClientRole() called with: CLIENT_ROLE_BROAD setclient = $client")

        }

    }

    fun leaveChannel() {
        rtcEngine?.leaveChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }


    private fun removeNotifications() {
        try {
            mNotificationManager?.cancelAll()
            stopForeground(true)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        joshAudioManager?.quitEverything()
        super.onTaskRemoved(rootIntent)
        isEngineInitialized = false
        retryInitLibrary = 0
        Log.d(TAG, "onTaskRemoved() called with: rootIntent = $rootIntent")
    }

    override fun onDestroy() {
        if (isRoomCreatedByUser) {
            endRoom(roomId, roomQuestionId)
        } else {
            leaveRoom(roomId, roomQuestionId)
        }
        Log.d(TAG, "onDestroy: isRoomCreatedByUser : $isRoomCreatedByUser ")
        RtcEngine.destroy()
        retryInitLibrary = 0
        isEngineInitialized = false
        joshAudioManager?.quitEverything()
        rtcEngine = null
        AppObjectController.mRtcEngine = null
        TelephonyUtil.getManager(this)
            .listen(hangUpRtcOnDeviceCallAnswered, PhoneStateListener.LISTEN_NONE)
        pstnCallState = CallState.CALL_STATE_IDLE
        executor.shutdown()
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
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

    private fun conversationRoomNotification(action : String): Notification {
        // TODO
        var isHavingPendingIntent = true
        if (action == InitLibrary().action){
            isHavingPendingIntent = false
        }
        Log.d(TAG, "conversationRoomNotification() called with: action = $action")
        mNotificationManager?.cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelName: CharSequence = "Voip Call Status"
            val mChannel = NotificationChannel(
                ROOM_NOTIFICATION_CHANNEL,
                notificationChannelName,
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Notifications for voice calling"
            }
            mNotificationManager?.createNotificationChannel(mChannel)
        }
        Log.d(
            TAG,
            "conversationRoomNotification: pending intent channel $conversationRoomChannelName"
        )

        val lNotificationBuilder =
            NotificationCompat.Builder(this, ROOM_NOTIFICATION_CHANNEL)
                .setChannelId(ROOM_NOTIFICATION_CHANNEL)
                .setContentTitle("Conversation Room")
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setColor(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.colorPrimary
                    )
                )
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)

        if (isHavingPendingIntent){
            val intent = ConversationLiveRoomActivity.getIntent(
                context = this,
                channelName = conversationRoomChannelName,
                uid = agoraUid,
                token = conversationRoomToken,
                isRoomCreatedByUser = isRoomCreatedByUser,
                roomId = roomId?.toInt(),
                moderatorId = moderatorUid,
                roomQuestionId = roomQuestionId,
                topicName = channelTopic
            )
            Log.d(TAG, "channelName: $conversationRoomChannelName")

            val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()

            val pendingIntent: PendingIntent =
                intent.let { notificationIntent ->
                    PendingIntent.getActivity(
                        this,
                        uniqueInt,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            lNotificationBuilder.setContentIntent(pendingIntent)
        }

        if (!conversationRoomTopicName.isNullOrEmpty()) {
            lNotificationBuilder.setContentText(conversationRoomTopicName)
        }

        lNotificationBuilder.priority = NotificationCompat.PRIORITY_MAX
        return lNotificationBuilder.build()
    }

}


sealed class WebRtcCalling
data class InitLibrary(val action: String = "calling.action.initLibrary") : WebRtcCalling()
data class IncomingCall(val action: String = "calling.action.incoming_call") : WebRtcCalling()
data class CallConnect(val action: String = "calling.action.connect") : WebRtcCalling()
data class CallDisconnect(val action: String = "calling.action.disconnect") : WebRtcCalling()
data class CallReject(val action: String = "calling.action.callReject") : WebRtcCalling()
data class DisableP2PAction(val action: String = "calling.action.disablep2p") : WebRtcCalling()

data class OutgoingCall(val action: String = "calling.action.outgoing_call") : WebRtcCalling()
data class CallStop(val action: String = "calling.action.stopcall") : WebRtcCalling()
data class CallForceConnect(val action: String = "calling.action.force_connect") : WebRtcCalling()
data class CallForceDisconnect(val action: String = "calling.action.force_disconnect") :
    WebRtcCalling()

data class NoUserFound(val action: String = "calling.action.no_user_found") :
    WebRtcCalling()

data class HoldCall(val action: String = "calling.action.hold_call") : WebRtcCalling()
data class ResumeCall(val action: String = "calling.action.resume_call") : WebRtcCalling()
data class UserJoined(val action: String = "calling.action.user_joined") : WebRtcCalling()
data class ConversationRoomJoin(val action: String = "calling.action.conversation_room_joined") :
    WebRtcCalling()

data class FavoriteIncomingCall(val action: String = "calling.action.favorite_incoming_call") :
    WebRtcCalling()

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
        const val ROOM_CALL_NOTIFICATION_ID = 200003
        const val CALL_NOTIFICATION_CHANNEL = "Call Notifications"
        const val LOCAL_NOTIFICATION_CHANNEL = "Local Notifications"
        const val ROOM_NOTIFICATION_CHANNEL = "Rooms Notifications"
    }
}
enum class CallState(val state: Int) {
    CALL_STATE_CONNECTED(0), CALL_STATE_IDLE(1), CALL_STATE_BUSY(2),
    CONNECT(3), DISCONNECT(4), REJECT(5), ONHOLD(6), UNHOLD(7), EXIT(8),
    WAITING_FOR_NETWORK(9), CALL_HOLD_BY_OPPOSITE(10), CALL_RESUME_BY_OPPOSITE(11)
}

interface ConversationRoomCallback {
    fun onUserOffline(uid: Int)
    fun onAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int
    )

    fun onSwitchToSpeaker()
    fun onSwitchToAudience()
    fun onIncomingCallConnected() {}
    fun onIncomingCallUserConnected() {}
    fun onNewIncomingCallChannel() {}
}
