package com.joshtalks.joshskills.core.firestore

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SERVER_TIME_OFFSET
import com.joshtalks.joshskills.core.notification.model.NotificationEvent
import timber.log.Timber

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
        val listOfReceived = notificationDao.getUnsyncEvent()

        if (listOfReceived?.isEmpty() == true) {
            return true
        }
        val serverOffsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET,true)
        val request = ArrayList<NotificationAnalyticsRequest>()
        listOfReceived?.forEach {
            request.add(NotificationAnalyticsRequest(it.id, it.time_stamp.plus(serverOffsetTime), it.action, it.platform))
        }
        if (request.isEmpty()) {
            return true
        }

        val resp = AppObjectController.utilsAPIService.engageNewNotificationAsync(request)
        if (resp.isSuccessful) {
            listOfReceived?.forEach {
                notificationDao.updateSyncStatus(it.notificationId)
            }
        } else {
            return false
        }
        return true
    }

    suspend fun pushAnalytics(event: NotificationEvent) {
        if (event.id.isBlank()) {
            return
        }
        try {
            notificationDao.insertNotificationEvent(event)
        } catch (e: Exception) {
            Timber.tag("Yash").e(e)
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
        API("API")
    }
}

