package com.joshtalks.joshskills.core.firestore

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.core.notification.model.NotificationEvent
import com.joshtalks.joshskills.base.local.model.Mentor
import com.joshtalks.joshskills.base.local.model.NotificationObject
import com.joshtalks.joshskills.util.showAppropriateMsg
import retrofit2.HttpException
import timber.log.Timber
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "NotificationAnalytics"

class NotificationAnalytics {
    private val notificationDao by lazy {
        AppObjectController.appDatabase.notificationEventDao()
    }

    suspend fun addAnalytics(notificationId: String, mEvent: Action, channel: Channel?): Boolean {
        Timber.tag(TAG).d("addAnalytics() called with: notificationId = $notificationId, mEvent = $mEvent, channel = $channel")
        var result = true
        var event = mEvent
        var platformChannel = channel?.action?: EMPTY
        val notification = getNotification(notificationId)
        if (notification != null && notification.isNotEmpty()) {
            if (event == Action.DISCARDED || event == Action.CLICKED) {
                notification.filter { it.action == Action.RECEIVED.action }[0].platform?.let {
                    platformChannel = it
                }
            } else if (event == Action.RECEIVED) {
                event = Action.APP_DISCARDED
            }
            result = false
        }
        val notificationEvent = NotificationEvent(
            action = event.action,
            time_stamp = System.currentTimeMillis(),
            platform = platformChannel,
            id = notificationId
        )
        pushAnalytics(notificationEvent)
        return result
    }

    suspend fun addAnalytics(notificationId: String, mEvent: Action, channel: String) {
        Timber.tag(TAG)
            .d("addAnalytics() called with: notificationId = $notificationId, mEvent = $mEvent, channel = $channel")
        val notification = getNotification(notificationId)
        if (notification != null && notification.isNotEmpty()) {
            return
        }
        val notificationEvent = NotificationEvent(
            action = mEvent.action,
            time_stamp = System.currentTimeMillis(),
            platform = channel,
            id = notificationId
        )
        pushAnalytics(notificationEvent)
    }

    suspend fun getNotification(notificationId: String): List<NotificationEvent>? {
        return notificationDao.getNotificationEvent(notificationId)
    }

    suspend fun pushToServer(): Boolean {
        try {
            val serverOffsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET, true)
            val listOfReceived = notificationDao.getUnsyncEvent()
            if (listOfReceived?.isEmpty() == true)
                return true

            listOfReceived?.forEach {
                try {
                    AppObjectController.utilsAPIService.engageNewNotificationAsync(
                        NotificationAnalyticsRequest(
                            it.id,
                            it.time_stamp.plus(serverOffsetTime),
                            it.action,
                            it.platform
                        )
                    )
                    notificationDao.updateSyncStatus(it.notificationId)
                } catch (e: Exception) {
                    if (e is HttpException) {
                        if (e.code() == 400)
                            notificationDao.updateSyncStatus(it.notificationId)
                    }
                    e.printStackTrace()
                }
            }
            return true
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
            try {
                FirebaseCrashlytics.getInstance().recordException(ex)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return false
        }
    }

    suspend fun fetchMissedNotification(context: Context) {
        try {
            val notifications = AppObjectController.utilsAPIService.getMissedNotifications(Mentor.getInstance().getId()).body()
            if (notifications?.isNotEmpty() == true) {
                for (item in notifications) {
                    val notificationTypeToken: Type = object : TypeToken<NotificationObject>() {}.type
                    val nc: NotificationObject = AppObjectController.gsonMapper.fromJson(
                        AppObjectController.gsonMapper.toJson(item),
                        notificationTypeToken
                    )
                    nc.contentTitle = item.title
                    nc.contentText = item.body
                    val isFirstTimeNotification = NotificationAnalytics().addAnalytics(
                        notificationId = nc.id.toString(),
                        mEvent = Action.RECEIVED,
                        channel = Channel.API
                    )
                    if (isFirstTimeNotification)
                        NotificationUtils(context).sendNotification(nc)
                }
            }
        } catch (e: Exception) {
            e.showAppropriateMsg()
            try {
                FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    suspend fun pushAnalytics(event: NotificationEvent) {
        if (event.id.isBlank()) {
            return
        }
        try {
            notificationDao.insertNotificationEvent(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    enum class Action(val action: String) {
        RECEIVED("received"),
        DISPLAYED("displayed"),
        CLICKED("clicked"),
        DISCARDED("discarded"),
        APP_DISCARDED("app_discarded")
    }

    enum class Channel(val action: String) {
        FCM("fcm"),
        FIRESTORE("firestore"),
        MOENGAGE("moengage"),
        API("API"),
        GROUPS("groups"),
        PUBNUB("PUBNUB")
    }
}

