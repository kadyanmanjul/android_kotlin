package com.joshtalks.joshskills.ui.voip

import android.annotation.SuppressLint
import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
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
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.conversationRoom.model.JoinConversionRoomRequest
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.firestore.AgoraNotificationListener
import com.joshtalks.joshskills.core.firestore.FirestoreDB
import com.joshtalks.joshskills.core.notification.FirebaseNotificationService
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ConvoRoomPointsEventBus
import com.joshtalks.joshskills.repository.local.eventbus.WebrtcEventBus
import com.joshtalks.joshskills.repository.local.model.FirestoreNotificationObject
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.ACTION_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CALL_NOTIFICATION_CHANNEL
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.analytics.CurrentCallDetails
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics.Event.DISCONNECT
import com.joshtalks.joshskills.ui.voip.analytics.VoipEvent
import com.joshtalks.joshskills.ui.voip.util.NotificationUtil
import com.joshtalks.joshskills.ui.voip.util.TelephonyUtil
import com.joshtalks.joshskills.util.DateUtils
import io.agora.rtc.Constants
import io.agora.rtc.Constants.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.collections.set


const val RTC_TOKEN_KEY = "token"
const val RTC_CHANNEL_KEY = "channel_name"
const val RTC_UID_KEY = "uid"
const val RTC_CALLER_UID_KEY = "caller_uid"
const val RTC_CALL_ID = "rtc_call_id"
const val RTC_NAME = "caller_name"
const val RTC_CALLER_PHOTO = "caller_photo"
const val RTC_IS_FAVORITE = "is_favorite"
const val RTC_IS_NEW_USER_CALL = "is_new_user_call"
const val RTC_IS_GROUP_CALL = "is_group_call"
const val RTC_IS_GROUP_CALL_ID = "is_group_call_id"
const val RTC_GROUP_CALL_GROUP_NAME = "group_call_group_name"
const val RTC_WEB_GROUP_CALL_GROUP_NAME = "group_web_call_group_name"
const val RTC_WEB_GROUP_PHOTO = "group_url"
const val RTC_PARTNER_ID = "partner_id"
const val DEFAULT_NOTIFICATION_TITLE = "Josh Skills App Running"
const val IS_CHANNEL_ACTIVE_KEY = "success"

class WebRtcService : BaseWebRtcService() {
    private val TAG = "ABCWebRtcService"
    private val log = Timber.tag("WebRtcService")
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
    private var oppositeCallerId: Int? = null
    private var userDetailMap: HashMap<String, String>? = null
    var speakingUsersNewList = arrayListOf<Int>()
    var speakingUsersOldList = arrayListOf<Int>()
    private val timber = Timber.tag(TAG)
    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    companion object {
        var isRoomEnded = false
        var incomingTimer: CoroutineScope? = null
        var conversationRoomTopicName: String? = ""
        private val TAG = WebRtcService::class.java.simpleName
        var pstnCallState = CallState.CALL_STATE_IDLE

        var isOnPstnCall = false
        var isConversionRoomActive = false
        var isRoomCreatedByUser = false
        var agoraUid: Int? = null
        var moderatorUid: Int? = null
        var roomId: String? = null
        var channelTopic: String? = null
        var roomQuestionId: Int? = null
        var conversationRoomChannelName: String? = null
        var conversationRoomToken: String? = null
        val incomingWaitJobsList = LinkedList<Job>()

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
        var currentCallingGroupName = ""

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

        fun tokenConformationApiFailed() {
            val data: HashMap<String, String?> = HashMap()
            data["uid"] = CurrentCallDetails.callieUid
            data["channel_name"] = CurrentCallDetails.channelName
            data["call_response"] = CallAction.DISCONNECT.action
            data["duration"] = "0"
            data["has_disconnected"] = "false"
            CoroutineScope(Dispatchers.IO).launch {
                AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
            }
        }

        @Volatile
        private var conversationRoomCallbackOld: WeakReference<ConversationRoomCallbackOld>? = null

        fun initLibrary() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = InitLibrary().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun cancelCallieDisconnectTimer() = try {
            incomingTimer?.cancel()
        } catch (e: Exception) {
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

        fun disconnectCall(reason: VoipEvent) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallDisconnect().action
                val reasonEnum = if (reason is VoipAnalytics.Event) reason else reason as DISCONNECT
                putExtra(DISCONNECT_REASON, reasonEnum)
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun disableP2P() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = DisableP2PAction().action
            }
            serviceIntent.startServiceForWebrtc()
        }

        fun disconnectCallFromCallie(isFromNotification: Boolean = true) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallStop().action
            }
            if (isFromNotification) {
                val state = CurrentCallDetails.state()
                VoipAnalytics.push(
                    DISCONNECT.CALL_DISCONNECT_NOTIFICATION_FAILURE,
                    agoraCallId = state.callId,
                    agoraMentorUid = state.callieUid,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
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

        fun declineDisconnect() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = CallDeclineDisconnect().action
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
        private val micMutex = Mutex()
        private val speakerMutex = Mutex()

        override fun onAudioVolumeIndication(
            speakers: Array<out AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            super.onAudioVolumeIndication(speakers, totalVolume)
            //log.d("${speakers}")
            val isSpeaking = (speakers?.find { it.uid == 0 }) != null
            val isListening = (speakers?.find { it.uid > 0 }) != null
            if (isSpeaking)
                CoroutineScope(Dispatchers.IO).launch {
                    saveMicState()
                }
            if (isListening)
                CoroutineScope(Dispatchers.IO).launch {
                    saveSpeakerState()
                }
        }

        suspend fun saveMicState() {
            //log.d("saveMicState: ")
            micMutex.withLock {
                //log.d("saveMicState --> lock")
                val currentCallDetails = CurrentCallDetails.state()
                //log.d("$currentCallDetails")
                val timestamp = DateUtils.getCurrentTimeStamp()
                if (!currentCallDetails.isSpeakingPushed && currentCallDetails.isCallConnectedScreenVisible) {
                    val agoraMentorUid = currentCallDetails.callieUid
                    val agoraCallId = currentCallDetails.callId

                    VoipAnalytics.push(
                        VoipAnalytics.Event.SPEAKING,
                        agoraMentorUid = agoraMentorUid,
                        agoraCallId = agoraCallId,
                        timeStamp = timestamp
                    )

                    if (agoraCallId.isNotEmpty() || agoraMentorUid.isNotEmpty())
                        CurrentCallDetails.speakingPushed()
                }
                //log.d("saveMicState --> unlock")
            }
        }

        suspend fun saveSpeakerState() {
            //log.d("saveSpeakerState")
            speakerMutex.withLock {
                //log.d("saveSpeakerState -- lock")
                val currentCallDetails = CurrentCallDetails.state()
                //log.d("$currentCallDetails")
                val timestamp = DateUtils.getCurrentTimeStamp()
                if (!currentCallDetails.isListeningPushed && currentCallDetails.isCallConnectedScreenVisible) {
                    val agoraMentorUid = currentCallDetails.callieUid
                    val agoraCallId = currentCallDetails.callId

                    VoipAnalytics.push(
                        VoipAnalytics.Event.LISTENING,
                        agoraMentorUid = agoraMentorUid,
                        agoraCallId = agoraCallId,
                        timeStamp = timestamp
                    )

                    if (agoraCallId.isNotEmpty() || agoraMentorUid.isNotEmpty())
                        CurrentCallDetails.listeningPushed()
                }
                //log.d("saveSpeakerState -- unlock")
            }
        }

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            Timber.tag(TAG).e("onAudioRouteChanged=  $routing")
            executor.submit {
                if (routing == AUDIO_ROUTE_HEADSET) {
                    bluetoothDisconnected()
                    callCallback?.get()?.onSpeakerOff()
                    isSpeakerEnabled = false
                    mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(isSpeakerEnabled)
                } else if (routing == AUDIO_ROUTE_HEADSETBLUETOOTH) {
                    callCallback?.get()?.onSpeakerOff()
                    isSpeakerEnabled = false
                    bluetoothConnected()
                } else {
                    bluetoothDisconnected()
                }
            }
        }

        private fun bluetoothDisconnected() {
            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        }

        private fun bluetoothConnected() {
            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
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
            cancelCallieDisconnectTimer()
            compositeDisposable.clear()
            userAgoraId = uid
            if (!(callType == CallType.INCOMING && WebRtcActivity.isIncomingCallHasNewChannel))
                isCallOnGoing.postValue(true)
            callData?.let {
                channelName = getChannelName(it)
                try {
                    val id = getUID(it)
                    if ((callType == CallType.INCOMING || callType == CallType.FAVORITE_INCOMING) && id == uid) {
                        if (!WebRtcActivity.isIncomingCallHasNewChannel) {
                            /*val state = CurrentCallDetails.state()
                            VoipAnalytics.push(
                                VoipAnalytics.Event.CALL_CONNECT_SCREEN_VISUAL,
                                agoraMentorUid = state.callieUid,
                                agoraCallId = state.callId,
                                timeStamp = DateUtils.getCurrentTimeStamp()
                            )
                            CurrentCallDetails.callConnectedScreenVisible()*/
                            startCallTimer()
                            if (isFavorite())
                                callStatusNetworkApi(it, CallAction.ACCEPT)
                            else
                                callCallback?.get()?.onIncomingCallConnected()
                            addNotification(CallConnect().action, callData)
                            //addSensor()
                            joshAudioManager?.startCommunication()
                        }
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
            CurrentCallDetails.reset()
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
                        endCall(
                            apiCall = true,
                            action = CallAction.DISCONNECT,
                            reason = DISCONNECT.AGORA_USER_OFFLINE_FAILURE
                        )
                    } else {
                        endCall(
                            apiCall = true,
                            action = CallAction.AUTO_DISCONNECT,
                            reason = DISCONNECT.AGORA_USER_OFFLINE_FAILURE
                        )
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
            cancelCallieDisconnectTimer()
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
                cancelCallieDisconnectTimer()
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

    @Volatile
    private var conversationRoomEventListener: IRtcEngineEventHandler? =
        object : IRtcEngineEventHandler() {

            override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                super.onJoinChannelSuccess(channel, uid, elapsed)
                Log.d(TAG, "joinChannelSuccess $uid")
                Log.d(TAG, "moderatorUid in onJoinChannelSuccess: $moderatorUid")

            }

            override fun onLeaveChannel(stats: RtcStats) {
                super.onLeaveChannel(stats)
            }

            override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                super.onRejoinChannelSuccess(channel, uid, elapsed)
                Log.d(TAG, "RejoinChannelSuccess $uid")

            }

            override fun onUserOffline(uid: Int, reason: Int) {
                super.onUserOffline(uid, reason)
                val isUserLeave = reason == USER_OFFLINE_QUIT
                if (!isRoomCreatedByUser && isUserLeave) {
                    conversationRoomCallbackOld?.get()?.onUserOffline(uid)
                }
                /*val usersReference = roomReference.document(roomId.toString()).collection("users")
                if (isRoomCreatedByUser) {
                    if (isUserLeave) {
                        usersReference.document(uid.toString()).delete()
                        Log.d(TAG, "isRoomCreatedByUser ${isRoomCreatedByUser} service OnUserOffline remove user by moderator $moderatorUid")
                    }
                } else {
                    if (uid == moderatorUid && isUserLeave) {
                        usersReference.get().addOnSuccessListener { documents ->
                            Log.d(TAG, "isRoomCreatedByUser ${isRoomCreatedByUser}  service OnUserOffline for moderator call $moderatorUid $reason")
                            if (documents.size() > 1) {
                                if (documents.documents[0].id.toInt() == agoraUid) {
                                    endRoom(roomId, roomQuestionId)
                                } else if (documents.documents[1].id.toInt() == agoraUid) {
                                    endRoom(roomId, roomQuestionId)
                                }
                            }

                        }
                    }
                }*/

            }

            override fun onAudioVolumeIndication(
                speakers: Array<out AudioVolumeInfo>?,
                totalVolume: Int
            ) {
                super.onAudioVolumeIndication(speakers, totalVolume)
                conversationRoomCallbackOld?.get()?.onAudioVolumeIndication(speakers, totalVolume)
                /*if (isRoomCreatedByUser) {
                    speakingUsersOldList.clear()
                    speakingUsersOldList.addAll(speakingUsersNewList)
                    speakingUsersNewList.clear()
                    speakers?.forEach {
                        if (it.uid != 0 && it.volume > 0) {
                            speakingUsersNewList.add(it.uid)
                        }
                        if (it.uid == 0 && it.volume > 0) {
                            speakingUsersNewList.add(agoraUid ?: 0)
                        }
                    }
                    updateFirestoreData()
                }*/
                /*try {
                    val user = speakers?.filter { it.uid == agoraUid!! }
                    if (user!=null && user.size!! > 0){
                        if (user[0].volume > 0) {
                            roomReference.document(roomId.toString()).collection("users").document(user[0].uid.toString()).update("is_speaking", true)
                        }
                        else {
                            roomReference.document(roomId.toString()).collection("users").document(user[0].uid.toString()).update("is_speaking", false)
                        }
                    }
                } catch (ex:Exception){
                    ex.printStackTrace()
                }*/
            }

        }


    fun endRoom(roomId: String?, conversationQuestionId: Int? = null) {
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
                    val request =
                        JoinConversionRoomRequest(
                            Mentor.getInstance().getId(),
                            roomId?.toInt() ?: 0,
                            qId
                        )
                    val response =
                        AppObjectController.conversationRoomsNetworkService.endConversationLiveRoom(
                            request
                        )
                    Log.d(TAG, "end room api call ${response.code()}")
                    if (response.isSuccessful) {
                        isRoomEnded = false
                        PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, false)
                        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
                        RxBus2.publish(ConvoRoomPointsEventBus(null))
                        conversationRoomChannelName = null
                        mRtcEngine?.leaveChannel()
                        //joshAudioManager?.endCommunication()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun leaveRoom(roomId: String?, conversationQuestionId: Int? = null) {
        if (roomId.isNullOrBlank().not()) {
            CoroutineScope(Dispatchers.IO).launch {
                if (isRoomEnded.not()) {
                    removeNotifications()
                    try {
                        var qId: Int? = null
                        if (conversationQuestionId != null && (conversationQuestionId != 0 || conversationQuestionId != -1)) {
                            qId = conversationQuestionId
                        }
                        val request =
                            JoinConversionRoomRequest(
                                Mentor.getInstance().getId(),
                                roomId?.toInt() ?: 0,
                                qId
                            )
                        val response =
                            AppObjectController.conversationRoomsNetworkService.leaveConversationLiveRoom(
                                request
                            )
                        Log.d(TAG, "leave room api call")
                        if (response.isSuccessful) {
                            isRoomEnded = false
                            PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, false)
                            PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
                            RxBus2.publish(ConvoRoomPointsEventBus(null))
                            conversationRoomChannelName = null
                            mRtcEngine?.leaveChannel()
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
            mRtcEngine?.leaveChannel()
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
                        cancelCallieDisconnectTimer()
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
            Timber.tag(TAG)
                .e("RTC=    %s isConversionRoomActive = %s", state, isConversionRoomActive)
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (isConversionRoomActive) {
                        //TODO recheck this code
                        /*val usersReference =
                            roomReference.document(roomId.toString()).collection("users")
                        usersReference.document(agoraUid.toString()).get().addOnSuccessListener {
                            val isMicOn = it["is_mic_on"]
                            val isSpeaker = it["is_speaker"]
                            if (isSpeaker == true && isMicOn == true) {
                                unMuteCall()
                            }
                        }*/
                    } else {
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
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (isConversionRoomActive) {
                        if (isRoomCreatedByUser) {
                            Log.d(
                                TAG,
                                "CALL_STATE_OFFHOOK  called with: state = $state, phoneNumber = $phoneNumber"
                            )
                            endRoom(roomId, roomQuestionId)
                        } else {
                            leaveRoom(roomId, roomQuestionId)
                        }
                    } else {
                        isOnPstnCall = true
                        val message = Message()
                        message.what = CallState.ONHOLD.state
                        mHandler?.sendMessage(message)
                    }

                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    if (isConversionRoomActive) {
                        /*val usersReference =
                            roomReference.document(roomId.toString()).collection("users")
                        usersReference.document(agoraUid.toString()).get().addOnSuccessListener {
                            val isMicOn = it["is_mic_on"]
                            val isSpeaker = it["is_speaker"]
                            if (isSpeaker == true && isMicOn == true) {
                                muteCall()
                            }
                        }*/
                    }
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
                if (nc.action == NotificationAction.INCOMING_CALL_NOTIFICATION)
                    nc.actionData?.let { VoipAnalytics.pushIncomingCallAnalytics(it) }
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
            Log.d(
                TAG,
                "initEngine called room id : $roomId isRoomCreatedByUser $isRoomCreatedByUser agoraUid:" +
                        " $agoraUid moderatorUid: $moderatorUid isConversionRoomActive: $isConversionRoomActive"
            )

            when (isConversionRoomActive) {
                false -> {
                    if (eventListener != null) {
                        mRtcEngine?.removeHandler(eventListener)
                    }
                    if (eventListener != null) {
                        mRtcEngine?.addHandler(eventListener)
                    }
                }
                true -> {
                    if (conversationRoomEventListener != null) {
                        mRtcEngine?.removeHandler(conversationRoomEventListener)
                    }
                    if (conversationRoomEventListener != null) {
                        mRtcEngine?.addHandler(conversationRoomEventListener)
                    }
                }
            }

            if (isEngineInitialized) {
                callback.invoke()
                return
            }
            mRtcEngine?.apply {
                if (BuildConfig.DEBUG) {
                    //setParameters("{\"rtc.log_filter\": 65535}")
                    //setParameters("{\"che.audio.start_debug_recording\":\"all\"}")
                }
                setParameters("{\"rtc.peer.offline_period\":$callReconnectTime}")
                setParameters("{\"che.audio.keep.audiosession\":true}")
                if (isConversionRoomActive) {
                    setParameters("{\"che.audio.enable.aec\":true}")
                }


                disableVideo()
                enableAudio()
                enableAudioVolumeIndication(500, 3, true)
                setAudioProfile(
                    AUDIO_PROFILE_SPEECH_STANDARD,
                    AUDIO_SCENARIO_EDUCATION
                )

                val statusCode = setChannelProfile(CHANNEL_PROFILE_COMMUNICATION)
                Log.d(TAG, "initEngine() called after setting Profile ${statusCode}")
                adjustRecordingSignalVolume(400)
                val audio = getSystemService(AUDIO_SERVICE) as AudioManager
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

    @Suppress("UNCHECKED_CAST")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand=  %s", intent?.action)
        isConversionRoomActive = intent?.action == ConversationRoomJoin().action
        if (!isConversionRoomActive) {
            executor.execute {
                intent?.action?.run {
                    removeActionNotifications()
                    if (this == DisableP2PAction().action) {
                        addNotification(DisableP2PAction().action, null)
                        removeInstance()
                        return@execute
                    }
                    initEngine {
                        try {
                            callForceDisconnect = false
                            when {
                                this == InitLibrary().action -> {
                                    Timber.tag(TAG).e("LibraryInit")
                                    addNotification(InitLibrary().action, null)
                                }
                                this == IncomingCall().action -> {
                                    if (CallState.CALL_STATE_BUSY == pstnCallState || isCallOnGoing.value == true) {
                                        return@initEngine
                                    }
                                    val data =
                                        intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                    data.let {
                                        callData = it
                                        CurrentCallDetails.fromMap(it)
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
                                        CurrentCallDetails.fromMap(it)
                                    }
                                    callType = CallType.OUTGOING
                                    joinCall(data)
                                }
                                this == CallConnect().action -> {
                                    val state = CurrentCallDetails.state()
                                    VoipAnalytics.push(
                                        VoipAnalytics.Event.CALL_ACCEPT,
                                        agoraMentorUid = state.callieUid,
                                        agoraCallId = state.callId,
                                        timeStamp = DateUtils.getCurrentTimeStamp()
                                    )
                                    removeIncomingNotification()
                                    val callData: HashMap<String, String?>? =
                                        intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>?
                                    addNotification(CallForceConnect().action,null)
                                    callConnectService(callData)
                                }
                                this == CallReject().action -> {
                                    addNotification(CallDisconnect().action, null)
                                    callData?.let {
                                        callStatusNetworkApi(it, CallAction.DECLINE)
                                        rejectCall()
                                    }
                                    val state = CurrentCallDetails.state()
                                    VoipAnalytics.push(
                                        VoipAnalytics.Event.CALL_DECLINED,
                                        agoraMentorUid = state.callieUid,
                                        agoraCallId = state.callId,
                                        timeStamp = DateUtils.getCurrentTimeStamp()
                                    )
                                    disconnectService()
                                }
                                this == CallDisconnect().action -> {
                                    addNotification(CallDisconnect().action, null)
                                    val reasonEnum =
                                        intent.getSerializableExtra(DISCONNECT_REASON)
                                    val reason =
                                        if (reasonEnum is VoipAnalytics.Event) reasonEnum else reasonEnum as DISCONNECT
                                    callData?.let {
                                        callStatusNetworkApi(
                                            it,
                                            CallAction.DISCONNECT,
                                            hasDisconnected = true
                                        )
                                    }
                                    endCall(reason = reason)
                                }
                                this == NoUserFound().action -> {
                                    callData?.let {
                                        callStatusNetworkApi(it, CallAction.NOUSERFOUND)
                                    }
                                    mRtcEngine?.leaveChannel()
                                    if (callCallback?.get() == null) {
                                        val state = CurrentCallDetails.state()
                                        VoipAnalytics.push(
                                            DISCONNECT.NO_USER_FOUND_FAILURE,
                                            agoraCallId = state.callId,
                                            agoraMentorUid = state.callieUid,
                                            timeStamp = DateUtils.getCurrentTimeStamp()
                                        )
                                    }
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
                                    endCall(
                                        apiCall = false,
                                        reason = DISCONNECT.FORCE_DISCONNECT_NOTIFICATION_FAILURE
                                    )
                                    RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                                }
                                this == CallDeclineDisconnect().action -> {
                                    stopRing()
                                    callForceDisconnect = true
                                    if (JoshApplication.isAppVisible.not()) {
                                        addNotification(CallDisconnect().action, null)
                                    }
                                    endCall(
                                        apiCall = false,
                                        reason = DISCONNECT.CALL_DECLINE_NOTIFICATION_FAILURE
                                    )
                                    RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
                                }
                                this == CallForceConnect().action -> {
                                    stopRing()
                                    callStartTime = 0L
                                    cancelCallieDisconnectTimer()
                                    compositeDisposable.clear()
                                    switchChannel = true
                                    setOppositeUserInfo(null)
                                    if (isCallOnGoing.value == true) {
                                        mRtcEngine?.leaveChannel()
                                    }
                                    resetConfig()
                                    addNotification(CallForceConnect().action, null)
                                    callData = null
                                    CurrentCallDetails.reset()
                                    AppObjectController.uiHandler.postDelayed(
                                        {
                                            val data =
                                                intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String?>
                                            callData = data
                                            CurrentCallDetails.fromMap(data)
                                            if (data.containsKey(RTC_CHANNEL_KEY)) {
                                                channelName = data[RTC_CHANNEL_KEY]
                                            }
                                            removeIncomingNotification()
                                            if (WebRtcActivity.isIncomingCallHasNewChannel) {
                                                joinCall(data, isNewChannelGiven = true)
                                            } else if (callCallback != null && callCallback?.get() != null && !WebRtcActivity.isIncomingCallHasNewChannel) {
                                                Log.d(TAG, "onStartCommand: CallForceConnect -->")
                                                callCallback?.get()?.switchChannel(data)
                                            } else {
                                                Log.d(TAG, "onStartCommand: ")
                                                startAutoPickCallActivity(
                                                    false,
                                                    isFromForceConnect = true
                                                )
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
                                    cancelCallieDisconnectTimer()
                                    compositeDisposable.clear()
                                    val message = Message()
                                    message.what = CallState.CALL_RESUME_BY_OPPOSITE.state
                                    mHandler?.sendMessage(message)
                                }
                                this == UserJoined().action -> {
                                    val uid =
                                        intent.getIntExtra(OPPOSITE_USER_UID, -1)
                                    if (uid != -1) {
                                        userJoined(uid, true)
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun removeInstance() {
        Log.d(TAG, "removeInstance() called")
        RtcEngine.destroy()
        isEngineInitialized = false
        AppObjectController.mRtcEngine = null
        retryInitLibrary = 0
        removeNotifications()
    }

    fun addListener(callback: WebRtcCallback?) {
        callCallback = WeakReference(callback)
    }

    fun addListener(callbackOld: ConversationRoomCallbackOld) {
        conversationRoomCallbackOld = WeakReference(callbackOld)
    }

    private fun callStopWithoutIssue() {
        callCallback?.get()?.onDisconnect(callId, callData?.let { getChannelName(it) })
        val message = Message()
        message.what = CallState.EXIT.state
        mHandler?.sendMessageDelayed(message, 1000)
        while (WebRtcService.incomingWaitJobsList.isNotEmpty())
            WebRtcService.incomingWaitJobsList.pop().cancel()
        disconnectService()
    }

    fun userJoined(uid: Int, isFromOnResume: Boolean = false) {
        removeIncomingNotification()
        oppositeCallerId = uid
        cancelCallieDisconnectTimer()
        compositeDisposable.clear()
        isCallOnGoing.postValue(true)
        isCallerJoined = true
        if (!isFavorite())
            callCallback?.get()?.onIncomingCallUserConnected()
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
        stopPlaying()
        audioFocus()
    }

    private fun audioFocus() {
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
            CurrentCallDetails.fromMap(it)
        }
        removeIncomingNotification()
        startAutoPickCallActivity(true)
    }

    fun removeIncomingNotification() {
        mNotificationManager?.cancel(ACTION_NOTIFICATION_ID)
        mNotificationManager?.cancel(INCOMING_CALL_NOTIFICATION_ID)
    }

    fun removeActionNotifications() {
        mNotificationManager?.cancel(ACTION_NOTIFICATION_ID)
    }

    private fun startAutoPickCallActivity(autoPick: Boolean, isFromForceConnect: Boolean = false) {
        val callActivityIntent =
            Intent(
                this,
                WebRtcActivity::class.java
            ).apply {
                callData?.apply {
                    if (isFavorite()) {
                        put(RTC_IS_FAVORITE, "true")
                    }
                    if (isGroupCall()) {
                        put(RTC_IS_GROUP_CALL, "true")
                    }
                    if (isNewUserCall()) {
                        put(RTC_IS_NEW_USER_CALL, "true")
                    }
                }
                if (isFromForceConnect)
                    putExtra(HIDE_INCOMING_UI, true)
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
                if (isGroupCall()) {
                    put(RTC_IS_GROUP_CALL, "true")
                }
                if (isNewUserCall()) {
                    put(RTC_IS_NEW_USER_CALL, "true")
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
        val state = CurrentCallDetails.state()
        VoipAnalytics.push(
            VoipAnalytics.Event.INCOMING_SCREEN_VISUAL,
            agoraMentorUid = state.callieUid,
            agoraCallId = state.callId,
            timeStamp = DateUtils.getCurrentTimeStamp()
        )
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
                if (isGroupCall()) {
                    put(RTC_IS_GROUP_CALL, "true")
                }
                if (isNewUserCall()) {
                    put(RTC_IS_NEW_USER_CALL, "true")
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
        Log.d(TAG, "addTimeObservable: ")
        cancelCallieDisconnectTimer()
        WebRtcActivity.isTimerCanceled = false
        incomingTimer = CoroutineScope(Dispatchers.IO)
        try {
            incomingTimer?.launch {
                delay(30000)
                WebRtcActivity.isTimerCanceled = true
                if (isCallConnected().not() && isActive) {
                    isTimeOutToPickCall = true
                    callData?.let {
                        val state = CurrentCallDetails.state()
                        VoipAnalytics.push(
                            VoipAnalytics.Event.USER_DID_NOT_PICKUP_CALL,
                            agoraMentorUid = state.callieUid,
                            agoraCallId = state.callId,
                            timeStamp = DateUtils.getCurrentTimeStamp()
                        )
                    }
                    disconnectCallFromCallie(isFromNotification = false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                    endCall(apiCall = true, reason = DISCONNECT.RECONNECTING_FAILURE)
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

    fun endCall(
        apiCall: Boolean = false,
        action: CallAction = CallAction.DISCONNECT,
        reason: VoipEvent,
        hasDisconnected: Boolean = false
    ) {
        Timber.tag(TAG).e("call_status%s", mRtcEngine?.connectionState)
        if (apiCall) {
            if (apiCall && reason == DISCONNECT.BACK_BUTTON_FAILURE) {
                val data: HashMap<String, String?> = HashMap()
                data["uid"] = CurrentCallDetails.callieUid
                data["channel_name"] = CurrentCallDetails.channelName
                callStatusNetworkApi(data, action, hasDisconnected = true)
            } else
                callData?.let {
                    callStatusNetworkApi(it, action, hasDisconnected = hasDisconnected)
                }
        }

        joshAudioManager?.endCommunication()
        val state = CurrentCallDetails.state()
        VoipAnalytics.push(
            reason,
            agoraCallId = state.callId,
            agoraMentorUid = state.callieUid,
            timeStamp = DateUtils.getCurrentTimeStamp()
        )
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

    private fun showConversationRoomNotification() {
        showNotification(
            conversationRoomNotification(), ACTION_NOTIFICATION_ID
        )
    }

    fun answerCall(data: HashMap<String, String?>, callAcceptApi: Boolean = false) {
        executor.execute {
            try {
                stopRing()
                if (isFavorite() || !callAcceptApi) {
                    joinCall(data)
                    executeEvent(AnalyticsEvent.USER_ANSWER_EVENT_P2P.NAME)
                } else {
                    showNotification(actionNotification("Connecting Call"), ACTION_NOTIFICATION_ID)
                    callData?.let {
                        callStatusNetworkApi(
                            it,
                            CallAction.ACCEPT,
                            isForCheckingChannel = true,
                            dataForIncomingCall = data
                        )
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun joinCall(data: HashMap<String, String?>, isNewChannelGiven: Boolean = false) {
        Log.e(TAG, "joinCall: $data")
        if (!isNewChannelGiven && isTimeOutToPickCall) {
            isTimeOutToPickCall = false
            RxBus2.publish(WebrtcEventBus(CallState.DISCONNECT))
            return
        }
        if (callData == null) {
            callData = data
            CurrentCallDetails.fromMap(data)
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
                disconnectCall(DISCONNECT.AGORA_LIBRARY_FAILURE)
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

    private fun getGroupUrl(): String? {
        return callData?.get(RTC_WEB_GROUP_PHOTO)
    }

    fun isFavorite(): Boolean {
        if (callData != null && callData!!.containsKey(RTC_IS_FAVORITE)) {
            return true
        }
        return false
    }

    fun isGroupCall(): Boolean {
        if (callData != null && callData!!.containsKey(RTC_IS_GROUP_CALL))
            return true
        return false
    }

    fun isNewUserCall(): Boolean {
        if (callData != null && callData!!.containsKey(RTC_IS_NEW_USER_CALL)) {
            return true
        }
        return false
    }

    fun setAsFavourite() = callData?.put(RTC_IS_FAVORITE, "true")

    fun setAsGroupCall() = callData?.put(RTC_IS_GROUP_CALL, "true")

    fun setAsNewUserCall() = callData?.put(RTC_IS_NEW_USER_CALL, "true")

    private fun getCallerName() = callData?.get(RTC_NAME) ?: EMPTY

    private fun getGroupName() = callData?.get(RTC_WEB_GROUP_CALL_GROUP_NAME) ?: "EMPTY"

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

    fun muteCall() {
        mRtcEngine?.muteLocalAudioStream(true)
    }

    fun unMuteCall() {
        mRtcEngine?.muteLocalAudioStream(false)
    }

    fun setClientRole(role: Int) {
        mRtcEngine?.setClientRole(role)
    }

    fun leaveChannel() {
        mRtcEngine?.leaveChannel()
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
        return mBinder
    }

    fun switchAudioSpeaker() {
        executor.submit {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            isSpeakerEnabled = !isSpeakerEnabled
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            mRtcEngine?.setEnableSpeakerphone(isSpeakerEnabled)
//            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(isSpeakerEnable)
            audioManager.isSpeakerphoneOn = isSpeakerEnabled
        }
    }

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
        //eventListener = null
        isSpeakerEnabled = false
        isMicEnabled = true
        oppositeCallerId = null
        pstnCallState = CallState.CALL_STATE_IDLE
        cancelCallieDisconnectTimer()
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
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).e("OnTaskRemoved")
    }

    override fun onDestroy() {
        Log.d(TAG, "service onDestroy() called isConversionRoomActive:${isConversionRoomActive}")
        if (isConversionRoomActive) {
            if (isRoomCreatedByUser) {
                endRoom(roomId, roomQuestionId)
            } else {
                leaveRoom(roomId, roomQuestionId)
            }
            Log.d(TAG, "onDestroy: isRoomCreatedByUser : $isRoomCreatedByUser ")
        }
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
                DisableP2PAction().action , InitLibrary().action -> {
                    showNotification(
                        actionNotification("Josh Skills"),
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
        val groupName = incomingData?.get(RTC_WEB_GROUP_CALL_GROUP_NAME)
        val builder = NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL)
            .setContentTitle(if(groupName == null)  getString(R.string.p2p_title) else "${getString(R.string.p2p_title)} $groupName")
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
            Intent(AppObjectController.joshApplication, WebRtcService::class.java).apply {
                action = CallReject().action
            }
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

        val courseExpiryTime =
            PrefManager.getLongValue(com.joshtalks.joshskills.core.COURSE_EXPIRY_TIME_IN_MS)
        val isCourseBought = PrefManager.getBoolValue(IS_COURSE_BOUGHT)
        val answerActionIntent =
            if (isCourseBought.not() &&
                courseExpiryTime != 0L &&
                courseExpiryTime < System.currentTimeMillis()
            ) {
                FreeTrialPaymentActivity.getFreeTrialPaymentActivityIntent(
                    this,
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID),
                    courseExpiryTime,
                    true
                )
            } else {
                Intent(this, WebRtcService::class.java)
                    .apply {
                        action = CallConnect().action
                        putExtra(CALL_USER_OBJ, incomingData)
                    }
            }

        val answerPendingIntent: PendingIntent = if (isCourseBought.not() &&
            courseExpiryTime != 0L &&
            courseExpiryTime < System.currentTimeMillis()
        ) {
            val uniqueRequestCode = (System.currentTimeMillis() and 0xfffffff).toInt()
            val inboxIntent =
                InboxActivity.getInboxIntent(this)
            inboxIntent.putExtra(HAS_NOTIFICATION, true)
            //inboxIntent.putExtra(NOTIFICATION_ID, notificationObject.id)
            PendingIntent.getActivities(
                this,
                uniqueRequestCode,
                arrayOf(inboxIntent, answerActionIntent),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(this, 0, answerActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }


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
        val avatar: Bitmap? = getIncomingCallAvatar(isFavorite = isFavorite(), isFromGroup = isGroupCall())
        val customView = getRemoteViews(isFavorite = isFavorite(), isFromGroup = isGroupCall())

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

    private fun getNameForGroupImage(): String {
        return try {
            callData?.get(RTC_WEB_GROUP_CALL_GROUP_NAME)?.substring(0, 2) ?: getRandomName()
        } catch (ex: Exception) {
            getRandomName()
        }
    }

    private fun getIncomingCallAvatar(isFavorite: Boolean, isFromGroup: Boolean): Bitmap? {
        return when {
            isFavorite -> getCallerUrl()?.urlToBitmap() ?: getNameForImage().textDrawableBitmap(width = 80, height = 80)
            isFromGroup -> getGroupUrl()?.urlToBitmap() ?: getNameForGroupImage().textDrawableBitmap(width = 80, height = 80)
            else -> getRandomName().textDrawableBitmap()
        }
    }

    private fun getRemoteViews(isFavorite: Boolean, isFromGroup: Boolean): RemoteViews {
        val layout = when{
            isFavorite -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                R.layout.favorite_call_notification_patch
            } else {
                R.layout.favorite_call_notification
            }
            isFromGroup -> R.layout.group_call_notification
            else -> R.layout.call_notification
        }

        val customView = RemoteViews(packageName, layout)
        customView.setTextViewText(
            R.id.name,
            when {
                isFavorite -> getString(R.string.favorite_p2p_title)
                isFromGroup -> getString(R.string.group_p2p_title)
                else -> getString(R.string.p2p_title)
            }
        )
        customView.setTextViewText(
            R.id.title,
            when {
                isFavorite -> getCallerName()
                isFromGroup -> getGroupName()
                else -> getString(R.string.p2p_subtitle)
            }
        )

        customView.setTextViewText(
            R.id.answer_text,
            getActionText(R.string.answer, R.color.action_color)
        )
        customView.setTextViewText(
            R.id.decline_text,
            getActionText(R.string.hang_up, R.color.error_color)
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
        val lNotificationBuilder =
            NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL)
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

        lNotificationBuilder.priority = NotificationCompat.PRIORITY_MAX
        return lNotificationBuilder.build()
    }

    private fun conversationRoomNotification(): Notification {
        Timber.tag(TAG).e("actionNotification  ")
        mNotificationManager?.cancelAll()
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
        Log.d(
            TAG,
            "conversationRoomNotification: pending intent channel $conversationRoomChannelName"
        )

        val lNotificationBuilder =
            NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL)
                .setChannelId(CALL_NOTIFICATION_CHANNEL)
                .setContentTitle("Conversation Room")
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setContentIntent(pendingIntent)
                .setColor(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.colorPrimary
                    )
                )
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
        if (!conversationRoomTopicName.isNullOrEmpty()) {
            lNotificationBuilder.setContentText(conversationRoomTopicName)
        }

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
                if (isGroupCall()) {
                    put(RTC_IS_GROUP_CALL, "true")
                }
                if (isNewUserCall()) {
                    put(RTC_IS_NEW_USER_CALL, "true")
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
        hasDisconnected: Boolean = false,
        isForCheckingChannel: Boolean = false,
        dataForIncomingCall: HashMap<String, String?> = HashMap()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val time = getTimeOfTalk()
//                data.remove("is_favorite")
//                data.remove("mentor_id")
//                data.remove("token")
//                data.remove("is_new_user_call")
//                data.remove("course_id")

                data["call_response"] = callAction.action
                data["duration"] = TimeUnit.MILLISECONDS.toSeconds(time).toString()
                data["has_disconnected"] = hasDisconnected.toString()
                if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
                    data["is_demo"] =
                        PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false).toString()
                }
                val resp = AppObjectController.p2pNetworkService.getAgoraCallResponse(data)
                if (isForCheckingChannel) {
                    val job = launch(Dispatchers.Main) {
                        if (resp.code() == 200) {
                            Log.d(TAG, "callStatusNetworkApi: ${resp.body()}")
                            val response = resp.body()
                            if (isActive) {
                                if (response?.containsKey(IS_CHANNEL_ACTIVE_KEY) == true) {
                                    joinCall(data)
                                    executeEvent(AnalyticsEvent.USER_ANSWER_EVENT_P2P.NAME)
                                } else {
                                    if (response?.containsKey(RTC_CHANNEL_KEY) == true) {
                                        val newChannel = response[RTC_CHANNEL_KEY]
                                        val token = response[RTC_TOKEN_KEY]
                                        val uid = response[RTC_UID_KEY]
                                        data[RTC_CHANNEL_KEY] = newChannel
                                        data[RTC_TOKEN_KEY] = token
                                        data[RTC_UID_KEY] = uid
                                        val oldState = CurrentCallDetails.state()
                                        VoipAnalytics.push(
                                            VoipAnalytics.Event.RECEIVE_TIMER_STOP,
                                            agoraMentorUid = oldState.callieUid,
                                            agoraCallId = oldState.callId,
                                            timeStamp = DateUtils.getCurrentTimeStamp()
                                        )
                                        CurrentCallDetails.set(
                                            channelName = newChannel ?: "",
                                            callId = response["agora_call_id"] ?: "",
                                            callieUid = uid ?: "",
                                            callerUid = ""
                                        )
                                        callCallback?.get()?.onNewIncomingCallChannel()
                                        joinCall(data, isNewChannelGiven = true)
                                        executeEvent(AnalyticsEvent.USER_ANSWER_EVENT_P2P.NAME)
                                    }
                                }
                            }
                        }
                    }
                    incomingWaitJobsList.push(job)
                    Log.d(TAG, "callStatusNetworkApi: Below Coroutine")
                } else {
                    if (resp.code() == 500) {
                        callCallback?.get()?.onNoUserFound()
                        return@launch
                    }
                    if (CallAction.ACCEPT == callAction) {
                        callCallback?.get()?.onServerConnect()
                        return@launch
                    }
                }
            } catch (ex: Exception) {
                val state = CurrentCallDetails.state()
                VoipAnalytics.push(
                    DISCONNECT.AGORA_CALL_RESPONSE_FAILURE,
                    agoraCallId = state.callId,
                    agoraMentorUid = state.callieUid,
                    timeStamp = DateUtils.getCurrentTimeStamp()
                )
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
data class UserJoined(val action: String = "calling.action.resume_call") : WebRtcCalling()
data class ConversationRoomJoin(val action: String = "calling.action.conversation_room_joined") :
    WebRtcCalling()

data class FavoriteIncomingCall(val action: String = "calling.action.favorite_incoming_call") :
    WebRtcCalling()

data class CallDeclineDisconnect(val action:String = "calling.action.decline_disconnect"):WebRtcCalling()

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
        const val ROOM_CALL_NOTIFICATION_ID = 200003
        const val CALL_NOTIFICATION_CHANNEL = "Call Notifications"
        const val LOCAL_NOTIFICATION_CHANNEL = "Local Notifications"
        const val ROOM_NOTIFICATION_CHANNEL = "Rooms Notifications"
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
    fun onIncomingCallConnected() {}
    fun onIncomingCallUserConnected() {}
    fun onNewIncomingCallChannel() {}
}

interface ConversationRoomCallbackOld {
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
