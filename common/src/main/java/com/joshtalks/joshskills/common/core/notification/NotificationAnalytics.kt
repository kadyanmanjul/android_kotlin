package com.joshtalks.joshskills.common.core.notification

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.firestore.NotificationAnalyticsRequest
import com.joshtalks.joshskills.common.core.notification.model.NotificationEvent
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.NotificationObject
import com.joshtalks.joshskills.common.util.showAppropriateMsg
import java.lang.reflect.Type

private const val TAG = "NotificationAnalytics"

class NotificationAnalytics {
    private val notificationDao by lazy {
        AppObjectController.appDatabase.notificationEventDao()
    }

    private val scheduledDao by lazy {
        AppObjectController.appDatabase.scheduleNotificationDao()
    }

    suspend fun addAnalytics(notificationId: String, mEvent: Action, channel: Channel?): Boolean {
        try {
            var result = true
            var event = mEvent
            var platformChannel = channel?.action ?: EMPTY
            val notification = getNotification(notificationId)
            if (notification != null && notification.isNotEmpty()) {
                if (event == Action.DISCARDED || event == Action.CLICKED) {
                    notification.filter { it.action == Action.RECEIVED.action || it.action == Action.DISPLAYED.action }[0].platform?.let {
                        platformChannel = it
                    }
                } else if (event == Action.RECEIVED) {
                    event = Action.APP_DISCARDED
                }
                result = false
            }
            if (event == Action.DISPLAYED) {
                scheduledDao.updateEventSent(notificationId)
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
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    suspend fun addAnalytics(notificationId: String, mEvent: Action, channel: String) {
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
            if (!PrefManager.getBoolValue(HAVE_CLEARED_NOTIF_ANALYTICS, isConsistent = false, defValue = false)) {
                notificationDao.clearEventsData()
                PrefManager.put(HAVE_CLEARED_NOTIF_ANALYTICS, true)
            }
            val serverOffsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET, true)
            val listOfReceived = notificationDao.getUnsyncEvent()
            if (listOfReceived?.isEmpty() == true)
                return true

            val request = ArrayList<NotificationAnalyticsRequest>()
            listOfReceived?.forEach {
                request.add(NotificationAnalyticsRequest(it.id, it.time_stamp.plus(serverOffsetTime), it.action, it.platform))
            }
            if (request.isEmpty()) {
                return true
            }

            AppObjectController.utilsAPIService.engageNewNotificationAsync(request)
            listOfReceived?.forEach {
                notificationDao.updateSyncStatus(it.notificationId)
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
                        AppObjectController.navigator.with(context).navigate(object : NotificationContract {
                            override val notificationObject = nc
                            override val navigator = AppObjectController.navigator
                        })
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
        API("API"),
        GROUPS("groups"),
        PUBNUB("PUBNUB"),
        CLIENT("client")
    }
}

