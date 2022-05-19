package com.joshtalks.joshskills.core.firestore

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.notification.model.NotificationEvent
import kotlinx.coroutines.CancellationException

private const val TAG = "NotificationAnalytics"

class NotificationAnalytics {
    private val notificationDao by lazy {
        AppObjectController.appDatabase.notificationEventDao()
    }

    suspend fun addAnalytics(notificationId: String, mEvent: Action, channel: Channel?): Boolean {
        var result = true
        var event = mEvent
        var platformChannel = channel?.name
        val notification = getNotification(notificationId)
        if (notification != null && notification.isNotEmpty()) {
            if (event == Action.DISCARDED || event == Action.CLICKED) {
                notification.filter { it.action == Action.RECEIVED.name }[0].platform?.let {
                    platformChannel = it
                }
            } else if (event == Action.RECEIVED) {
                event = Action.APP_DISCARDED
            }
            result = false
        }
        val notificationEvent = NotificationEvent(
            action = event.name,
            time_stamp = System.currentTimeMillis(),
            platform = platformChannel,
            id = notificationId
        )
        pushAnalytics(notificationEvent)
        return result
    }

    suspend fun getNotification(notificationId: String): List<NotificationEvent>? {
        return notificationDao.getNotificationEvent(notificationId)
    }

    suspend fun pushToServer(): Boolean {
        val listOfReceived = notificationDao.getUnsyncEvent()
        if (listOfReceived?.isEmpty() == true) {
            return true
        }

        val request = ArrayList<NotificationAnalyticsRequest>()
        listOfReceived?.forEach {
            request.add(NotificationAnalyticsRequest(it.id, it.time_stamp, it.action, it.platform))
        }
        if (request.isEmpty()) {
            return true
        }

        val resp = AppObjectController.commonNetworkService.engageNewNotificationAsync(request)
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
            if (e is CancellationException)
                throw e
            e.printStackTrace()
        }
    }

    enum class Action(action: String) {
        RECEIVED("received"),
        DISPLAYED("displayed"),
        CLICKED("clicked"),
        DISCARDED("discarded"),
        APP_DISCARDED("app_discarded")
    }

    enum class Channel(action: String) {
        FCM("fcm"),
        FIRESTORE("firestore"),
        MOENGAGE("moengage")
    }
}

