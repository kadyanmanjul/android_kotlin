package com.joshtalks.joshskills.core.notification

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
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.DismissNotifEventReceiver
import com.joshtalks.joshskills.core.firestore.FirestoreDB
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.voip.*
import com.joshtalks.joshskills.ui.voip.analytics.VoipAnalytics
import com.joshtalks.joshskills.util.Utils
import com.moengage.core.LogLevel
import com.moengage.core.internal.logger.Logger
import com.moengage.mi.MoEMiPushHelper
import com.moengage.mi.internal.*
import com.xiaomi.mipush.sdk.*
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService

class MiPushReceiver : PushMessageReceiver() {
    private val tag = "MiPushReceiver"

    override fun onReceivePassThroughMessage(context: Context?, miPushMessage: MiPushMessage?) {
        try {
            Logger.print { "$tag onReceivePassThroughMessage() : Will try to process and show pass through message." }
            if (miPushMessage == null || context == null) {
                Logger.print(LogLevel.WARN) { "$tag onReceivePassThroughMessage() : Context or Mi Push object is null." }
                return
            }
            if (!MoEMiPushHelper.getInstance().isFromMoEngagePlatform(miPushMessage)) {
//                notifyNonMoEngagePush(context, miPushMessage, NotifyType.PASS_THROUGH_MESSAGE)
                return
            }
            MoEMiPushHelper.getInstance().passPushPayload(context, miPushMessage)
        } catch (e: Exception) {
            Logger.print(LogLevel.ERROR, e) { "$tag onReceivePassThroughMessage() : " }
        }
    }

    override fun onNotificationMessageClicked(context: Context?, message: MiPushMessage?) {
        try {
            if (message == null || context == null) {
                Logger.print(LogLevel.WARN) { "$tag onNotificationMessageClicked() : MiPushMessage object is null" }
                return
            }
//            if (!MoEMiPushHelper.getInstance().isFromMoEngagePlatform(message)) {
//                notifyNonMoEngagePush(context, message, NotifyType.NOTIFICATION_CLICK)
//                return
//            }
            MoEMiPushHelper.getInstance().onNotificationClicked(context, message)
        } catch (e: Exception) {
            Logger.print(LogLevel.ERROR, e) { "$tag onNotificationMessageClicked() : " }
        }
    }

    override fun onReceiveRegisterResult(
        context: Context?,
        message: MiPushCommandMessage?
    ) {
        // save push token
        try {
            Timber.tag(tag).e("2 : $message")
            if (message == null || context == null) return
            val command = message.command
            if (MiPushClient.COMMAND_REGISTER != command) {
                Logger.print { "$tag onReceiveRegisterResult() : Received command is not register command." }
                return
            }
            if (message.resultCode != ErrorCode.SUCCESS.toLong()) {
                Logger.print { "$tag onReceiveRegisterResult() : Registration failed." }
                return
            }
            val arguments = message.commandArguments ?: return
            val pushToken = if (arguments.size > 0) arguments[0] else null
            if (pushToken.isNullOrEmpty()) {
                Logger.print { "$tag onReceiveRegisterResult() : Token is null or empty." }
                return
            }
        } catch (e: Exception) {
            Logger.print(LogLevel.ERROR, e) { "$tag onReceiveRegisterResult() : " }
        }
    }

    override fun onRequirePermissions(context: Context?, strings: Array<String>?) {
        try {
            Logger.print { "$tag onRequirePermissions() : $strings" }
        } catch (e: Exception) {
            Logger.print(LogLevel.ERROR, e) { "$tag onRequirePermissions() : " }
        }
    }

    override fun onNotificationMessageArrived(context: Context?, miPushMessage: MiPushMessage?) {
        try {
            if (context == null || miPushMessage == null) return
            val map = Utils.jsonToMap(JSONObject(miPushMessage.content).getJSONObject("gcm_alert"))
            processRemoteMessage(context, map)
            logNotificationReceived(context, miPushMessage)
        } catch (e: Exception) {
            Logger.print(LogLevel.ERROR, e) { "$tag onNotificationMessageArrived() : " }
        }
    }

    private fun processRemoteMessage(context: Context, remoteData: MutableMap<String, String>) {
        if (remoteData.containsKey("nType")) {
            val notificationTypeToken: Type = object : TypeToken<ShortNotificationObject>() {}.type
            val shortNc: ShortNotificationObject = AppObjectController.gsonMapper.fromJson(
                AppObjectController.gsonMapper.toJson(remoteData),
                notificationTypeToken
            )

            FirestoreDB.getNotification {
                val nc = it.toNotificationObject(shortNc.id)
                if (remoteData["nType"] == "CR") {
                    nc.actionData?.let {
                        VoipAnalytics.pushIncomingCallAnalytics(it)
                    }
                }
                sendNotification(context, nc)
            }
        }
    }

    private fun sendNotification(context: Context, notificationObject: NotificationObject) {
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

                val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val pendingIntent = PendingIntent.getActivities(
                    context.applicationContext,
                    uniqueInt, activityList,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val style = NotificationCompat.BigTextStyle()
                style.setBigContentTitle(notificationObject.contentTitle)
                style.bigText(notificationObject.contentText)
                style.setSummaryText("")

                val notificationBuilder = NotificationCompat.Builder(
                    context.applicationContext,
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
                            context,
                            R.color.colorAccent
                        )
                    )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.priority = NotificationManager.IMPORTANCE_DEFAULT
                }

                val dismissIntent =
                    Intent(context.applicationContext, DismissNotifEventReceiver::class.java).apply {
                        putExtra(NOTIFICATION_ID, notificationObject.id)
                        putExtra(HAS_NOTIFICATION, true)
                    }
                val dismissPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(context.applicationContext, uniqueInt, dismissIntent, 0)

                notificationBuilder.setDeleteIntent(dismissPendingIntent)

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

                notificationManager.notify(uniqueInt, notificationBuilder.build())
            }
        }
    }

    private fun getIntentAccordingAction(
        notificationObject: NotificationObject,
        action: NotificationAction?,
        actionData: String?
    ): Intent? {
        return when (action) {
            NotificationAction.ACTION_LOGOUT_USER -> {
                if (Mentor.getInstance().hasId() && User.getInstance().isVerified) {
                    Mentor.deleteUserCredentials(true)
                    Mentor.deleteUserData()
                }
                return null
            }
            NotificationAction.INCOMING_CALL_NOTIFICATION -> {
                if (!PrefManager.getBoolValue(
                        PREF_IS_CONVERSATION_ROOM_ACTIVE
                    ) && !PrefManager.getBoolValue(USER_ACTIVE_IN_GAME)
                ) {
                    incomingCallNotificationAction(notificationObject.actionData)
                }
                return null
            }
            NotificationAction.CALL_DISCONNECT_NOTIFICATION -> {
                callDisconnectNotificationAction()
                return null
            }
            NotificationAction.CALL_FORCE_CONNECT_NOTIFICATION -> {
                callForceConnect(notificationObject.actionData)
                return null
            }
            NotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION -> {
                callForceDisconnect()
                return null
            }
            NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION -> {
                WebRtcService.noUserFoundCallDisconnect()
                return null
            }
            NotificationAction.CALL_ON_HOLD_NOTIFICATION -> {
                WebRtcService.holdCall()
                return null
            }
            NotificationAction.CALL_RESUME_NOTIFICATION -> {
                WebRtcService.resumeCall()
                return null
            }
            NotificationAction.CALL_CONNECTED_NOTIFICATION -> {
                if (notificationObject.actionData != null) {
                    try {
                        val obj = JSONObject(notificationObject.actionData!!)
                        WebRtcService.userJoined(obj.getInt(OPPOSITE_USER_UID))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                return null
            }
            else -> return null
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

        fun sendFirestoreNotification(
            notificationObject: NotificationObject,
            context: Context
        ) {
            executor.execute {

                val intent = getIntentForNotificationAction(
                    notificationObject,
                    notificationObject.action,
                    notificationObject.actionData
                )

                intent?.run {
                    putExtra(HAS_NOTIFICATION, true)
                    putExtra(NOTIFICATION_ID, notificationObject.id)

                    val activityList = arrayOf(this)
                    val uniqueInt = (System.currentTimeMillis() and 0xfffffff).toInt()
                    val defaultSound =
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val pendingIntent = PendingIntent.getActivities(
                        context.applicationContext,
                        uniqueInt, activityList,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val style = NotificationCompat.BigTextStyle()
                    style.setBigContentTitle(notificationObject.contentTitle)
                    style.bigText(notificationObject.contentText)
                    style.setSummaryText("")

                    val notificationBuilder =
                        NotificationCompat.Builder(
                            context,
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
                                    context,
                                    R.color.colorAccent
                                )
                            )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        notificationBuilder.priority = NotificationManager.IMPORTANCE_DEFAULT
                    }

                    val dismissIntent =
                        Intent(
                            context.applicationContext,
                            DismissNotifEventReceiver::class.java
                        ).apply {
                            putExtra(NOTIFICATION_ID, notificationObject.id)
                            putExtra(HAS_NOTIFICATION, true)
                        }
                    val dismissPendingIntent: PendingIntent =
                        PendingIntent.getBroadcast(
                            context.applicationContext,
                            uniqueInt,
                            dismissIntent,
                            0
                        )

                    notificationBuilder.setDeleteIntent(dismissPendingIntent)

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
                    notificationManager.notify(uniqueInt, notificationBuilder.build())
                }
                if (PrefManager.getStringValue(API_TOKEN).isNotEmpty()) {
                    EngagementNetworkHelper.receivedNotification(notificationObject)
                }
            }
        }

        private fun getIntentForNotificationAction(
            notificationObject: NotificationObject,
            action: NotificationAction?,
            actionData: String?
        ): Intent? {
            return when (action) {
                NotificationAction.INCOMING_CALL_NOTIFICATION -> {
                    if (!PrefManager.getBoolValue(PREF_IS_CONVERSATION_ROOM_ACTIVE)
                        && !PrefManager.getBoolValue(USER_ACTIVE_IN_GAME)
                    ) {
                        incomingCallNotificationAction(notificationObject.actionData)
                    }
                    null
                }
                NotificationAction.CALL_DISCONNECT_NOTIFICATION -> {
                    callDisconnectNotificationAction()
                    null
                }
                NotificationAction.CALL_FORCE_CONNECT_NOTIFICATION -> {
                    callForceConnect(notificationObject.actionData)
                    null
                }
                NotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION -> {
                    callForceDisconnect()
                    null
                }
                NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION -> {
                    WebRtcService.noUserFoundCallDisconnect()
                    null
                }
                NotificationAction.CALL_ON_HOLD_NOTIFICATION -> {
                    WebRtcService.holdCall()
                    null
                }
                NotificationAction.CALL_RESUME_NOTIFICATION -> {
                    WebRtcService.resumeCall()
                    null
                }
                NotificationAction.CALL_CONNECTED_NOTIFICATION -> {
                    if (notificationObject.actionData != null) {
                        try {
                            val obj = JSONObject(notificationObject.actionData!!)
                            WebRtcService.userJoined(obj.getInt(OPPOSITE_USER_UID))
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                    null
                }
                else -> null
            }
        }

        private fun callForceDisconnect() {
            WebRtcService.forceDisconnect()
        }

        private fun callForceConnect(actionData: String?) {
            actionData?.let {
                try {
                    val obj = JSONObject(it)
                    val data = HashMap<String, String>()
                    data[RTC_TOKEN_KEY] = obj.getString("token")
                    data[RTC_CHANNEL_KEY] = obj.getString("channel_name")
                    data[RTC_UID_KEY] = obj.getString("uid")
                    data[RTC_CALLER_UID_KEY] = obj.getString("caller_uid")
                    try {
                        data[RTC_CALL_ID] = obj.getString("agoraCallId")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (obj.has("group_name"))
                        data[RTC_WEB_GROUP_CALL_GROUP_NAME] = obj.getString("group_name")

                    if (obj.has("is_group_call"))
                        data[RTC_IS_GROUP_CALL] = obj.getString("is_group_call")

                    if (obj.has("group_url"))
                        data[RTC_WEB_GROUP_PHOTO] = obj.getString("group_url")

                    WebRtcService.currentCallingGroupName =
                        data[RTC_WEB_GROUP_CALL_GROUP_NAME] ?: ""
                    WebRtcService.forceConnect(data)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }

        private fun callDisconnectNotificationAction() {
            WebRtcService.disconnectCallFromCallie()
        }

        private fun incomingCallNotificationAction(actionData: String?) {
            actionData?.let {
                try {
                    val obj = JSONObject(it)
                    val data = HashMap<String, String?>()
                    data[RTC_TOKEN_KEY] = obj.getString("token")
                    data[RTC_CHANNEL_KEY] = obj.getString("channel_name")
                    data[RTC_UID_KEY] = obj.getString("uid")
                    data[RTC_CALLER_UID_KEY] = obj.getString("caller_uid")
                    try {
                        data[RTC_CALL_ID] = obj.getString("agoraCallId")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (obj.has("group_name"))
                        data[RTC_WEB_GROUP_CALL_GROUP_NAME] = obj.getString("group_name")

                    if (obj.has("is_group_call"))
                        data[RTC_IS_GROUP_CALL] = obj.getString("is_group_call")

                    if (obj.has("group_url"))
                        data[RTC_WEB_GROUP_PHOTO] = obj.getString("group_url")

                    if (obj.has("f")) {
                        val id = obj.getInt("caller_uid")
                        val caller =
                            AppObjectController.appDatabase.favoriteCallerDao()
                                .getFavoriteCaller(id)
                        Thread.sleep(25)
                        if (caller != null) {
                            data[RTC_NAME] = caller.name
                            data[RTC_CALLER_PHOTO] = caller.image
                            data[RTC_IS_FAVORITE] = "true"
                        }
                    }
                    WebRtcService.startOnNotificationIncomingCall(data)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }
}