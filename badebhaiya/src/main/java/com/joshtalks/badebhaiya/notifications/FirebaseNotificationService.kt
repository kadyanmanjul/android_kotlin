package com.joshtalks.badebhaiya.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.NotificationObject
import com.joshtalks.badebhaiya.core.NotificationAction
import com.joshtalks.badebhaiya.core.IS_CONVERSATION_ROOM_ACTIVE_FOR_USER
import com.joshtalks.badebhaiya.core.PREF_IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.startServiceForWebrtc
import com.joshtalks.badebhaiya.core.NotificationChannelNames
import com.joshtalks.badebhaiya.core.JoshSkillExecutors
import com.joshtalks.badebhaiya.core.analytics.DismissNotifEventReceiver
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.FCMData
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.ApiRespStatus
import com.joshtalks.badebhaiya.utils.Utils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService

const val FCM_TOKEN = "fcmToken"
const val HAS_NOTIFICATION = "has_notification"
const val NOTIFICATION_ID = "notification_id"

class FirebaseNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(FirebaseNotificationService::class.java.name).e(token)
        try {
            if (PrefManager.hasKey(FCM_TOKEN)) {
                val fcmResponse = FCMData.getInstance()
                fcmResponse?.apiStatus = ApiRespStatus.POST
                fcmResponse?.update()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        PrefManager.put(FCM_TOKEN, token)
        postFCMToken(token)
    }

    private fun postFCMToken(token: String) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO +
                CoroutineExceptionHandler { _, _ -> }).launch {
            val userId = User.getInstance().userId
            if (userId.isNotBlank()) {
                try {
                    if (PrefManager.hasKey(FCM_TOKEN)) {
                        FCMData.getInstance()?.let { fcmData ->
                            val data = mutableMapOf(
                                "registration_id" to token
                            )
                            val resp = CommonRepository().patchFCMToken(fcmData.id, data)
                            if (resp.isSuccessful) {
                                resp.body()?.update()
                                Timber.tag(FCMData::class.java.name)
                                    .e("patch data : ${resp.body()}")
                            }
                        }
                    } else {
                        val data = mutableMapOf(
                            "name" to Utils.getDeviceName(),
                            "registration_id" to token,
                            "device_id" to Utils.getDeviceId(),
                            "active" to "true",
                            "user_id" to userId,
                            "type" to "android"
                        )
                        val resp = CommonRepository().postFCMToken(data)
                        if (resp.isSuccessful) {
                            resp.body()?.update()
                            Timber.tag(FCMData::class.java.name).e("post data : ${resp.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("PUSH NOTIFICATION AGAYI AUR DATA HAI => ${remoteMessage.data}")
        super.onMessageReceived(remoteMessage)
        Timber.tag(FirebaseNotificationService::class.java.name).e("fcm")
        try {
            if (BuildConfig.DEBUG) {
                Timber.tag(FirebaseNotificationService::class.java.simpleName).e(
                    Gson().toJson(remoteMessage.data)
                )
            }
            val notificationTypeToken: Type =
                object : TypeToken<NotificationObject>() {}.type
            val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
                AppObjectController.gsonMapper.toJson(remoteMessage.data),
                notificationTypeToken
            )
            sendNotification(nc)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun sendNotification(notificationObject: NotificationObject) {
        executor.execute {
            val intent = getIntentAccordingAction(
                notificationObject,
                notificationObject.action,
                notificationObject.actionData
            )

            intent?.run {
                putExtra(HAS_NOTIFICATION, true)
                putExtra(NOTIFICATION_ID, notificationObject.id)

                val activityList = arrayOf(this)

                if (notificationObject.action == NotificationAction.JOIN_CONVERSATION_ROOM) {
                    val obj = JSONObject(notificationObject.actionData)
                    val name = obj.getString("moderator_name")
                    val topic = obj.getString("topic")
                    notificationObject.contentTitle = getString(R.string.room_title)
                    notificationObject.contentText =
                        getString(R.string.convo_notification_title, name, topic)
                }

                val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val pendingIntent = PendingIntent.getActivities(
                    applicationContext,
                    uniqueInt, activityList,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val style = NotificationCompat.BigTextStyle()
                style.setBigContentTitle(notificationObject.contentTitle)
                style.bigText(notificationObject.contentText)
                style.setSummaryText("")

                val notificationBuilder =
                    NotificationCompat.Builder(
                        this@FirebaseNotificationService,
                        notificationChannelId
                    )
                        .setTicker(notificationObject.ticker)
                        .setSmallIcon(R.drawable.ic_status_bar_notification)
                        .setContentTitle(notificationObject.contentTitle)
                        .setAutoCancel(true)
                        .setSound(defaultSound)
                        .setContentText(notificationObject.contentText)
                        .setContentIntent(pendingIntent)
                        .setStyle(style)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setColor(
                            ContextCompat.getColor(
                                this@FirebaseNotificationService,
                                R.color.colorAccent
                            )
                        )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.priority = NotificationManager.IMPORTANCE_DEFAULT
                }

                val dismissIntent =
                    Intent(applicationContext, DismissNotifEventReceiver::class.java).apply {
                        putExtra(NOTIFICATION_ID, notificationObject.id)
                        putExtra(HAS_NOTIFICATION, true)
                    }
                val dismissPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(applicationContext, uniqueInt, dismissIntent, 0)

                notificationBuilder.setDeleteIntent(dismissPendingIntent)

                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationChannel = NotificationChannel(
                        notificationChannelId,
                        notificationChannelName,
                        importance
                    )
                    notificationChannel.enableLights(true)
                    notificationChannel.enableVibration(true)
                    notificationBuilder.setChannelId(notificationChannelId)
                    notificationManager.createNotificationChannel(notificationChannel)
                }

                when(notificationObject.action) {
                    else -> notificationManager.notify(uniqueInt, notificationBuilder.build())
                }
            }
        }
    }

    private fun getIntentAccordingAction(
        notificationObject: NotificationObject,
        action: NotificationAction?,
        actionData: String?
    ): Intent? {
        return when(action) {
            NotificationAction.ACTION_LOGOUT_USER -> {
                if (User.getInstance().userId.isNotEmpty()) {
                    User.deleteUserCredentials(true)
                }
                return null
            }
            NotificationAction.JOIN_CONVERSATION_ROOM -> {
                if (!PrefManager.getBoolValue(PREF_IS_CONVERSATION_ROOM_ACTIVE) && User.getInstance().userId.isNotEmpty()
                    && PrefManager.getBoolValue(IS_CONVERSATION_ROOM_ACTIVE_FOR_USER)
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(this, HeadsUpNotificationService::class.java).apply {
                            putExtra(ConfigKey.ROOM_DATA, actionData)
                        }
                        intent.startServiceForWebrtc()
                    } else {
                        val roomId = JSONObject(actionData).getString("room_id")
                        val topic = JSONObject(actionData).getString("topic") ?: EMPTY

                        if (roomId.isNotBlank()) {
//                            return ConversationLiveRoomActivity.getIntentForNotification(
//                                AppObjectController.joshApplication,
//                                roomId, topicName = topic
//                            )
                        } else return null
                    }
                }
                return null
            }
            NotificationAction.ROOM_IS_ABOUT_TO_START -> {
                Intent(
                    AppObjectController.joshApplication,
                    ProfileFragment::class.java
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.userId)
                }
            }
            else -> null
        }
    }

    companion object {
        private val executor: ExecutorService by lazy {
            JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Notification")
        }

        private var notificationChannelId = "101111"
        private var notificationChannelName = NotificationChannelNames.DEFAULT.type

        @RequiresApi(Build.VERSION_CODES.N)
        private var importance = NotificationManager.IMPORTANCE_DEFAULT
    }
}
