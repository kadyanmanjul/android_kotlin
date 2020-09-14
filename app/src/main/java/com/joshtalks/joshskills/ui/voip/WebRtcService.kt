package com.joshtalks.joshskills.ui.voip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_RING
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.voip.extra.FullScreenActivity
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchClientListener
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClientListener
import com.sinch.gson.JsonElement
import com.sinch.gson.JsonParser
import timber.log.Timber
import java.util.concurrent.ExecutorService

class WebRtcService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private val mBinder: IBinder = MyBinder()
    private val INCOMING_CALL_CHANNEL_ID = "incoming_call_channel_id"
    private val CALL_NOTIFICATION_ID = 200001
    private var sinchClient: SinchClient? = null
    private var call: Call? = null

    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Calling Service")

    companion object {
        private val TAG = "SinchCallingListenService"

        @Volatile
        var isCallWasOnGoing: Boolean = false

        fun onIncomingCall(data: Map<String, String>) {
            Timber.tag(TAG).e("OnIncomingCall")

            val pq = AppObjectController.sinchClient?.relayRemotePushNotificationPayload(
                data
            )
            var iData: String? = pq?.callResult?.headers?.get("data")
            if (iData == null) {
                if (pq?.displayName.equals("private", ignoreCase = true)) {
                    return
                } else {
                    iData = pq?.displayName ?: EMPTY
                }
            }

            val serviceIntent = Intent(
                AppObjectController.joshApplication,
                WebRtcService::class.java
            ).apply {
                action = IncomingCallNotification().action
                putExtra(INCOMING_CALL_JSON_OBJECT, iData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && JoshApplication.isAppVisible.not()) {
                serviceIntent.also { intent ->
                    AppObjectController.joshApplication.startForegroundService(intent)
                }
            } else {
                AppObjectController.joshApplication.startService(serviceIntent)
            }
        }
    }

    private var sinchClientListener = object : SinchClientListener {
        override fun onClientStarted(client: SinchClient) {
            Timber.tag(TAG).e("onClientStarted")
        }

        override fun onClientStopped(client: SinchClient) {
            Timber.tag(TAG).e("onClientStopped")
        }


        override fun onClientFailed(client: SinchClient, error: SinchError) {
            Timber.tag(TAG).e("onClientFailed")

        }

        override fun onRegistrationCredentialsRequired(
            p0: SinchClient,
            registrationCallback: ClientRegistration
        ) {
            Timber.tag(TAG).e("onRegistrationCredentialsRequired")

        }

        override fun onLogMessage(level: Int, area: String, message: String) {
            Timber.tag(TAG).e("Sinch Log %s", message)
        }

    }

    private var callClientListener =
        CallClientListener { _, incomingCall ->
            isCallWasOnGoing = true
            call = incomingCall
            Timber.tag(TAG).e("Call Incoming%s", call)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && call == null) {
                Intent(
                    AppObjectController.joshApplication,
                    WebRtcService::class.java
                ).apply {
                    action = IncomingCall().action
                    putExtra(
                        INCOMING_CALL_JSON_OBJECT, incomingCall.headers["data"]
                    )
                }.also { intent ->
                    AppObjectController.joshApplication.startForegroundService(intent)
                }
            } else {
                val i = Intent()
                i.setClass(this, WebRtcActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                i.putExtra(INCOMING_CALL_JSON_OBJECT, incomingCall.headers["data"])
                startActivity(i)
            }
        }

    override fun onCreate() {
        super.onCreate()
            if (AppObjectController.sinchClient == null || sinchClient == null) {
                sinchClient = AppObjectController.initSinchClient()
            }
        executor.execute {
            mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            sinchClient?.let { sinchClient ->
                if (sinchClient.isStarted.not()) {
                    sinchClient.setSupportActiveConnectionInBackground(true)
                    sinchClient.start()
                }
                sinchClient.addSinchClientListener(sinchClientListener)
                sinchClient.callClient?.addCallClientListener(callClientListener)
                sinchClient.startListeningOnActiveConnection()
            }
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).e("StartService")
        if (intent?.action == null || sinchClient == null) {
            return START_NOT_STICKY
        }

        executor.execute {
            intent.action?.run {
                Timber.tag(TAG).e(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                when {
                    this == IncomingCallNotification().action -> {
                        getSinchCall(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val notification = incomingCallNotification(
                                intent.getStringExtra(INCOMING_CALL_JSON_OBJECT)
                            )
                            mNotificationManager?.notify(
                                CALL_NOTIFICATION_ID, notification
                            )
                            startForeground(CALL_NOTIFICATION_ID, notification)
                        } else {
                            processIncomingCall(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                        }
                    }
                    this == IncomingCall().action -> {
                        getSinchCall(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                        processIncomingCall(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                    }
                    this == CallDisconnect().action -> {
                        stopForeground(true)
                        mNotificationManager?.cancel(CALL_NOTIFICATION_ID)
                        call?.hangup()
                    }
                    this == CallConnect().action -> {
                        stopForeground(true)
                        mNotificationManager?.cancel(CALL_NOTIFICATION_ID)
                        processIncomingCall(intent.getStringExtra(INCOMING_CALL_JSON_OBJECT))
                    }
                    this == OutgoingCall().action -> {
                        Timber.tag(TAG).e("Outgoing")
                    }
                }
            }

        }
        return START_STICKY
    }

    private fun processIncomingCall(data: String?) {
        val backIntent = Intent(
            AppObjectController.joshApplication,
            InboxActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val callActivityIntent =
            Intent(AppObjectController.joshApplication, WebRtcActivity::class.java).apply {
                putExtra(IS_INCOMING_CALL, true)
                putExtra(INCOMING_CALL_JSON_OBJECT, data)
            }

        if (AppObjectController.currentActivityClass == null) {
            AppObjectController.joshApplication.startActivities(
                arrayOf(
                    backIntent,
                    callActivityIntent
                )
            )
        } else {
            AppObjectController.currentActivityClass?.run {
                if (this.equals(WebRtcActivity::class.java.simpleName, ignoreCase = true)
                        .not()
                ) {
                    AppObjectController.joshApplication.startActivities(
                        arrayOf(
                            backIntent,
                            callActivityIntent
                        )
                    )
                }
            }
        }
    }

    private fun getSinchCall(data: String?) {
        val jsonElement: JsonElement = JsonParser().parse(data)
        jsonElement.asJsonObject.get("mentor_id")?.asString?.run {
            Timber.tag(TAG).e("%s%s", this, call)
            if (call == null) {
                call = sinchClient?.callClient?.getCall(this)
            }
        }
    }

    private fun getIncomingCallUserName(data: String?): String {
        val jsonElement: JsonElement = JsonParser().parse(data)
        return jsonElement.asJsonObject.get("name")?.asString ?: "JoshSkills User"
    }

    fun getCall() = call

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onDestroy() {
        Timber.tag(TAG).e("onDestroy")
    }

    inner class MyBinder : Binder() {
        fun getService(): WebRtcService {
            return this@WebRtcService
        }
    }

    private fun incomingCallNotification(data: String?): android.app.Notification {

        val incomingSoundUri: Uri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.incoming)

        val att: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mNotificationManager?.getNotificationChannel(
                INCOMING_CALL_CHANNEL_ID
            ) == null
        ) {
            val name: CharSequence = "Voip Incoming Call"
            val importance: Int = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(INCOMING_CALL_CHANNEL_ID, name, importance)
            mChannel.enableLights(true)
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = (longArrayOf(0, 1000, 500, 1000))
            mChannel.setSound(incomingSoundUri, att)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val notificationIntent = Intent(this, WebRtcActivity::class.java)
        notificationIntent.action = "calling.action.main"

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

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
        answerActionIntent.action = CallConnect().action
        val answerActionPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, answerActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val extras = Bundle()

        val lNotificationBuilder = NotificationCompat.Builder(this, INCOMING_CALL_CHANNEL_ID)
            .setContentTitle(getIncomingCallUserName(data))
            .setContentText("Incoming voice call")
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(true)
            .setExtras(extras)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.com_facebook_button_icon, "DECLINE", declineActionPendingIntent)
            .addAction(R.drawable.com_facebook_button_icon, "ANSWER", answerActionPendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(incomingSoundUri, STREAM_RING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        return lNotificationBuilder.build()
    }
/*
    private fun isAppVisible(): Boolean {
        return ProcessLifecycleOwner
            .get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.STARTED)
    }*/

}


sealed class Notification
data class IncomingCall(val action: String = "calling.action.incoming_call") : Notification()
data class IncomingCallNotification(val action: String = "calling.action.incoming_call_notification") :
    Notification()

data class CallConnect(val action: String = "calling.action.connect") : Notification()
data class CallDisconnect(val action: String = "calling.action.disconnect") : Notification()
data class OutgoingCall(val action: String = "calling.action.outgoing_call") : Notification()