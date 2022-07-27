package com.joshtalks.joshskills.core.notification

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.moengage.core.LogLevel
import com.moengage.core.internal.logger.Logger
import com.moengage.mi.MoEMiPushHelper
import com.moengage.mi.internal.logNotificationReceived
import com.xiaomi.mipush.sdk.ErrorCode
import com.xiaomi.mipush.sdk.MiPushClient
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Type

class MiPushReceiver : PushMessageReceiver() {
    private val tag = "MiPushReceiver"

    override fun onReceivePassThroughMessage(context: Context?, miPushMessage: MiPushMessage?) {
        try {
            Timber.tag(tag).e("1 : $miPushMessage")
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
        Timber.tag(tag).e("2 : $message")
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
            Timber.tag(tag).e("3 : $message")
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

            //pass the mi push token to moengage
            MoEMiPushHelper.getInstance().passPushToken(context, pushToken)

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
        Timber.tag(tag).e("4 : $miPushMessage")
        try {
            if (context == null || miPushMessage == null) return
            val remoteData = JSONObject(miPushMessage.content)
            val map = mutableMapOf<String, String>()
            //TODO : add other attributes (IMP)
            map["id"] = remoteData["id"].toString()
            //TODO: uncomment this line - processRemoteMessage(context, map)
            logNotificationReceived(context, miPushMessage)
        } catch (e: Exception) {
            Logger.print(LogLevel.ERROR, e) { "$tag onNotificationMessageArrived() : " }
        }
    }

    private fun processRemoteMessage(context: Context, remoteData: MutableMap<String, String>) {
        Timber.tag(tag).e("5 : $remoteData")
        val notificationTypeToken: Type = object : TypeToken<NotificationObject>() {}.type
        val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
            AppObjectController.gsonMapper.toJson(remoteData),
            notificationTypeToken
        )
        nc.contentTitle = remoteData["title"]
        nc.contentText = remoteData["body"]

        CoroutineScope(Dispatchers.IO).launch {
            val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
                notificationId = nc.id.toString(),
                mEvent = NotificationAnalytics.Action.RECEIVED,
                channel = NotificationAnalytics.Channel.MOENGAGE
            )
            if (isFirstTimeNotification)
                NotificationUtils(context).sendNotification(nc)
        }
    }
}