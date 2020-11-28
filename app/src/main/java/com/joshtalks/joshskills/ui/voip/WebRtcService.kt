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
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CallType
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.core.printAll
import com.joshtalks.joshskills.repository.local.model.UserPlivoDetailsModel
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_CHANNEL_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.CONNECTED_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.EMPTY_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_CHANNEL_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.INCOMING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.OUTGOING_CALL_CHANNEL_ID
import com.joshtalks.joshskills.ui.voip.NotificationId.Companion.OUTGOING_CALL_NOTIFICATION_ID
import com.joshtalks.joshskills.ui.voip.extra.FullScreenActivity
import com.joshtalks.joshskills.ui.voip.util.SoundPoolManager
import com.plivo.endpoint.Endpoint
import com.plivo.endpoint.EventListener
import com.plivo.endpoint.Incoming
import com.plivo.endpoint.Outgoing
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.concurrent.ExecutorService

class WebRtcService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private val mBinder: IBinder = MyBinder()
    private var endpoint: Endpoint? = null
    private var userPlivo: UserPlivoDetailsModel? = null
    private var callData: Any? = null
    private var options: HashMap<String, Any> = object : HashMap<String, Any>() {
        init {
            put("debug", BuildConfig.DEBUG)
            put("enableTracking", true)
            put("maxAverageBitrate", 21000)
        }
    }
    private var isSpeakerEnable = false
    private var isMicEnable = true
    private var callCallback: WeakReference<WebRtcCallback>? = null
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")

    private var incomingCallData: HashMap<String, String>? = null
    private var outgoingCallData: HashMap<String, String>? = null
    private var callFromFirebase = false

    companion object {
        private val TAG = "PlivoCallingListenService"

        @Volatile
        private var callUUID: String? = null

        @Volatile
        var isCallWasOnGoing: Boolean = false

        fun loginUserClient() {
            if (UserPlivoDetailsModel.getPlivoUser() == null) {
                return
            }
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = LoginUser().action
            }
            ContextCompat.startForegroundService(AppObjectController.joshApplication, serviceIntent)
        }

        fun logoutUserClient() {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = LogoutUser().action
            }
            ContextCompat.startForegroundService(AppObjectController.joshApplication, serviceIntent)
        }

        fun startOutgoingCall(map: HashMap<String, String?>) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = OutgoingCall().action
                putExtra(CALL_USER_OBJ, map)
            }
            ContextCompat.startForegroundService(AppObjectController.joshApplication, serviceIntent)
        }

        fun startOnNotificationIncomingCall(data: HashMap<String, String>) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = NotificationIncomingCall().action
                putExtra(INCOMING_CALL_USER_OBJ, data)
            }
            ContextCompat.startForegroundService(AppObjectController.joshApplication, serviceIntent)
        }

        fun startOnIncomingCall(data: HashMap<String, String>) {
            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = IncomingCall().action
                putExtra(INCOMING_CALL_USER_OBJ, data)
            }
            ContextCompat.startForegroundService(AppObjectController.joshApplication, serviceIntent)
        }
    }

    fun addListener(callback: WebRtcCallback?) {
        callCallback = WeakReference(callback)
    }

    private var eventListener = object : EventListener {
        override fun onLogin() {
            Timber.tag(TAG).e("LoginUser")
            Timber.tag(TAG).e("= %s", endpoint?.registered.toString())

            try {
                endpoint?.keepAlive()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)?.cancel(
                EMPTY_NOTIFICATION_ID
            )

            if (outgoingCallData != null) {
                initCall()
            }
            if (callFromFirebase && incomingCallData != null) {
                endpoint?.relayVoipPushNotification(incomingCallData)
                callFromFirebase = false
            }

        }

        override fun onLogout() {
            Timber.tag(TAG).e("LogOutUser")
        }

        override fun onLoginFailed() {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)?.cancel(
                EMPTY_NOTIFICATION_ID
            )
            Timber.tag(TAG).e("onLoginFailed")
        }

        override fun onIncomingDigitNotification(p0: String) {
            Timber.tag(TAG).e("onIncomingDigitNotification=  %s", p0)
        }

        override fun onIncomingCall(incoming: Incoming) {
            Timber.tag(TAG).e("onIncomingCall")
            callData = incoming
            startOnIncomingCall(incoming.headerDict as HashMap<String, String>)
            callUUID = incoming.headerDict["X-PH-MOBILEUUID"]
        }

        override fun onIncomingCallHangup(incoming: Incoming) {
            callData = incoming
            Timber.tag(TAG).e("%s%s", "onIncomingCallHangup ", getCallId())
            callCallback?.get()?.onCallDisconnect(getCallId())
            onDisconnectAndRemove()
        }

        //end user ne phone kaat diya
        override fun onIncomingCallRejected(incoming: Incoming) {
            Timber.tag(TAG).e("onIncomingCallRejected")
            callCallback?.get()?.onDisconnect(getCallId())
            callData = incoming
            callCallback?.get()?.onIncomingCallHangup(getCallId())
            onDisconnectAndRemove()
        }

        override fun onIncomingCallInvalid(p0: Incoming) {
            Timber.tag(TAG).e("onIncomingCallInvalid")
            onDisconnectAndRemove()

        }

        override fun onOutgoingCall(outgoing: Outgoing) {
            callData = outgoing
            //   callCallback?.get()?.initOutgoingCall(getCallId())
            Timber.tag(TAG).e("onOutgoingCall")
        }

        /*  override fun onOutgoingCallRinging(p0: Outgoing) {
              Timber.tag(TAG).e("onOutgoingCallRinging")
          }*/

        override fun onOutgoingCallAnswered(outgoing: Outgoing) {
            Timber.tag(TAG).e("onOutgoingCallAnswered")
            callData = outgoing
            callCallback?.get()?.onConnect()
            if (outgoingCallData.isNullOrEmpty().not()) {
                showNotificationConnectedCall(outgoingCallData as HashMap<String, String>)
            }
        }

        // samene wale ne phone kaat diya
        override fun onOutgoingCallRejected(outgoing: Outgoing) {
            callData = outgoing
            Timber.tag(TAG).e("onOutgoingCallRejected %s", getCallId())
            callCallback?.get()?.onCallReject(getCallId())
            removeNotifications()
        }

        // khud ne call kaata
        override fun onOutgoingCallHangup(outgoing: Outgoing) {
            callData = outgoing
            Timber.tag(TAG).e("onOutgoingCallHangup  %s", getCallId())
            callCallback?.get()?.onSelfDisconnect(getCallId())
            removeNotifications()
        }

        override fun onOutgoingCallInvalid(p0: Outgoing) {
            Timber.tag(TAG).e("onOutgoingCallInvalid")
        }

        override fun mediaMetrics(p0: HashMap<*, *>) {
            Timber.tag(TAG).e("mediaMetrics")
        }
    }

    inner class MyBinder : Binder() {
        fun getService(): WebRtcService {
            return this@WebRtcService
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).e("onCreate")
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        startForeground(EMPTY_NOTIFICATION_ID, loginUserService())
        endpoint = Endpoint.newInstance(BuildConfig.DEBUG, eventListener, options)
        executor.execute {
            userPlivo = UserPlivoDetailsModel.getPlivoUser()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("onStartCommand=  %s", intent?.action)
        if (intent?.action == null) {
            return START_NOT_STICKY
        }
        intent.action?.run {
            if (this == LoginUser().action) {
                startForeground(EMPTY_NOTIFICATION_ID, loginUserService())
            }
        }
        executor.execute {
            if (endpoint == null) {
                endpoint = Endpoint.newInstance(BuildConfig.DEBUG, eventListener, options)
            }
            if (userPlivo == null) {
                userPlivo = UserPlivoDetailsModel.getPlivoUser()
            }
            intent.action?.run {
                try {
                    Timber.tag(TAG).e(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                    when {
                        this == LoginUser().action -> {
                            Timber.tag(TAG).e("User= %s", userPlivo?.toString())
                            if (isUserLogin().not()) {
                                loginUser()
                            }
                            removeNotifications()
                        }
                        this == LogoutUser().action -> {
                            stopService()
                        }
                        this == NotificationIncomingCall().action -> {
                            incomingCallData =
                                intent.getSerializableExtra(INCOMING_CALL_USER_OBJ) as HashMap<String, String>?
                            if (isUserLogin()) {
                                endpoint?.relayVoipPushNotification(incomingCallData)
                            } else {
                                callFromFirebase = true
                                loginUser()
                            }
                        }
                        this == IncomingCall().action -> {
                            SoundPoolManager.getInstance(applicationContext).playRinging()
                            val incomingData: HashMap<String, String>? =
                                intent.getSerializableExtra(INCOMING_CALL_USER_OBJ) as HashMap<String, String>?
                            incomingData?.printAll()
                            showNotificationOnIncomingCall(incomingData)
                            processIncomingCall(incomingData)
                        }
                        this == OutgoingCall().action -> {
                            outgoingCallData =
                                intent.getSerializableExtra(CALL_USER_OBJ) as HashMap<String, String>
                            outgoingCallData?.printAll()
                            if (isUserLogin()) {
                                initCall()
                            } else {
                                loginUser()
                            }
                        }
                        this == CallConnect().action -> {
                            mNotificationManager?.cancel(INCOMING_CALL_NOTIFICATION_ID)
                            mNotificationManager?.cancel(OUTGOING_CALL_NOTIFICATION_ID)
                            val incomingData: HashMap<String, String>? =
                                intent.getSerializableExtra(INCOMING_CALL_USER_OBJ) as HashMap<String, String>?
                            val callActivityIntent =
                                Intent(this@WebRtcService, WebRtcActivity::class.java).apply {
                                    putExtra(CALL_TYPE, CallType.INCOMING)
                                    putExtra(AUTO_PICKUP_CALL, true)
                                    putExtra(CALL_USER_OBJ, incomingData)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            startActivities(arrayOf(callActivityIntent))
                        }
                        this == CallDisconnect().action -> {
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
        return START_STICKY
    }

    private fun initCall() {
        endpoint?.createOutgoingCall()
            ?.callH(userPlivo?.username, outgoingCallData)
        showNotificationOnOutgoingCall(outgoingCallData)
        callUUID = outgoingCallData?.get("X-PH-MOBILEUUID")
        outgoingCallData = null
        Timber.tag(TAG).e("Outgoing")
    }

    fun getCallId(): String? {
        return callUUID
    }

    private fun isUserLogin(): Boolean {
        if (userPlivo != null && endpoint != null && endpoint!!.registered) {
            return true
        }
        return false
    }

    private fun loginUser() {
        endpoint?.login(
            userPlivo?.username,
            userPlivo?.password,
            PrefManager.getStringValue(FCM_TOKEN)
        )
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
    }


    private fun showNotificationOnIncomingCall(incomingData: HashMap<String, String>?) {
        if (isAppVisible()) {
            val notification = incomingCallNotification(incomingData)
            startForeground(INCOMING_CALL_NOTIFICATION_ID, notification)
        }
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

    private fun showNotificationConnectedCall(incomingData: HashMap<String, String>?) {
        val notification = callConnectNotification(incomingData)
        startForeground(CONNECTED_CALL_NOTIFICATION_ID, notification)
    }


    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    fun answerCall() {
        callData?.run {
            try {
                if (this is Incoming) {
                    SoundPoolManager.getInstance(applicationContext).stopRinging()
                    this.answer()
                    showNotificationConnectedCall(this.headerDict as HashMap<String, String>)
                    callCallback?.get()?.onConnect()
                    return
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun endCall() {
        callData?.run {
            try {
                if (this is Outgoing) {
                    this.hangup()
                    return@run
                }

                if (this is Incoming) {
                    this.hangup()
                    return@run
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        onDisconnectAndRemove()
    }

    fun rejectCall() {
        callData?.run {
            try {
                if (this is Incoming) {
                    this.reject()
                    return@run
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        onDisconnectAndRemove()
    }

    fun holdCall() {
        callData?.run {
            if (this is Outgoing) {
                this.hold()
                return
            }
            if (this is Incoming) {
                this.hold()
                return
            }
        }
    }

    fun unHoldCall() {
        callData?.run {
            if (this is Outgoing) {
                this.unhold()
                return
            }
            if (this is Incoming) {
                this.unhold()
                return
            }
        }
    }

    private fun muteCall() {
        callData?.run {
            try {
                if (this is Outgoing) {
                    this.mute()
                    return
                }
                if (this is Incoming) {
                    this.mute()
                    return
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun unMuteCall() {
        callData?.run {
            try {
                if (this is Outgoing) {
                    this.unmute()
                    return
                }
                if (this is Incoming) {
                    this.unmute()
                    return
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun switchAudioSpeaker() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (isSpeakerEnable) {
            isSpeakerEnable = false
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } else {
            isSpeakerEnable = true
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        }
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
        callCallback?.get()?.onDisconnect(getCallId())
        callCallback = null
        removeNotifications()
        SoundPoolManager.getInstance(applicationContext)?.stopRinging()
        isCallWasOnGoing = false
        isSpeakerEnable = false
        isMicEnable = true

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
    }

    private fun removeNotifications() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)?.cancelAll()
        stopForeground(true)
    }

    fun getSpeaker() = isSpeakerEnable
    fun getMic() = isMicEnable


    private fun stopService() {
        plivoLogout()
        Timber.tag(TAG).e("stopService")
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        plivoLogout()
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).e("OnTaskRemoved")
    }

    override fun onDestroy() {
        Timber.tag(TAG).e("onDestroy")
    }

    private fun plivoLogout() {
        try {
            endpoint?.logout()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun loginUserService(): Notification {
        Timber.tag(TAG).e("Login User   ")

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
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        return lNotificationBuilder.build()
    }


    private fun incomingCallNotification(incomingData: HashMap<String, String>?): Notification {
        Timber.tag(TAG).e("incomingCallNotification   ")
        val incomingSoundUri: Uri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.incoming)

        val att: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Voip Incoming Call"
            val importance: Int = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(INCOMING_CALL_CHANNEL_ID, name, importance)
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
                    putExtra(INCOMING_CALL_USER_OBJ, incomingData)
                }

        val answerActionPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, answerActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val lNotificationBuilder = NotificationCompat.Builder(this, INCOMING_CALL_CHANNEL_ID)
            .setChannelId(INCOMING_CALL_CHANNEL_ID)
            .setContentTitle(incomingData?.get("X-PH-CALLERNAME"))
            .setContentText("Incoming voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)

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
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
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

    private fun callConnectNotification(incomingData: HashMap<String, String>?): Notification {
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
            .setContentTitle(incomingData?.get("X-PH-CALLERNAME"))
            .setContentText("Ongoing voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
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
}


sealed class WebRtcCalling
data class NotificationIncomingCall(val action: String = "calling.action.notification_incoming_call") :
    WebRtcCalling()

data class IncomingCall(val action: String = "calling.action.incoming_call") : WebRtcCalling()
data class CallConnect(val action: String = "calling.action.connect") : WebRtcCalling()
data class CallDisconnect(val action: String = "calling.action.disconnect") : WebRtcCalling()
data class OutgoingCall(val action: String = "calling.action.outgoing_call") : WebRtcCalling()
data class LoginUser(val action: String = "calling.action.login") : WebRtcCalling()
data class LogoutUser(val action: String = "calling.action.logout") : WebRtcCalling()
data class CallStop(val action: String = "calling.action.stopcall") : WebRtcCalling()

class NotificationId {
    companion object {
        val EMPTY_NOTIFICATION_ID = 200000
        val INCOMING_CALL_NOTIFICATION_ID = 200001
        val OUTGOING_CALL_NOTIFICATION_ID = 200002
        val CONNECTED_CALL_NOTIFICATION_ID = 200003
        val INCOMING_CALL_CHANNEL_ID = "incoming_call_channel_id"
        val OUTGOING_CALL_CHANNEL_ID = "outgoing_call_channel_id"
        val CONNECTED_CALL_CHANNEL_ID = "connected_call_channel_id"
        val EMPTY_CHANNEL_ID = "empty_channel_id"
    }
}

interface WebRtcCallback {
    fun onRinging()
    fun onConnect()
    fun onDisconnect(callId: String?)
    fun onCallDisconnect(id: String?)
    fun onCallReject(id: String?)
    fun onSelfDisconnect(id: String?)
    fun onIncomingCallHangup(id: String?)
//    fun initOutgoingCall(id: String?)

}
