package com.joshtalks.badebhaiya.core

import com.joshtalks.badebhaiya.core.analytics.AnalyticsEvent
import com.joshtalks.badebhaiya.core.analytics.AppAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val VIDEO_TIME_LEAP = 6

object EngagementNetworkHelper {

    fun receivedNotification(notificationObject: NotificationObject) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppAnalytics
                    .create(AnalyticsEvent.NOTIFICATION_RECEIVED.NAME)
                    .addParam("title", notificationObject.contentTitle)
                    .addParam("content", notificationObject.contentText)
                    .addParam("name", notificationObject.name)
                    .addParam("id", notificationObject.id)
                    .addParam("mentorId", notificationObject.mentorId)
                    .addParam("notificationType", notificationObject.type)
                    .addUserDetails()
                    .push()
                val data = mapOf("is_delivered" to "true")
                notificationObject.id?.let {
//                    AppObjectController.chatNetworkService.engageNotificationAsync(it, data)
                    Timber.d("notifEngage12 : ( $it , delivered)")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun clickNotification(notificationId: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (notificationId.isNullOrEmpty()) {
                    return@launch
                }
                AppAnalytics
                    .create(AnalyticsEvent.NOTIFICATION_CLICKED.NAME)
                    .addParam("id", notificationId)
//                    .addParam("mentorId", Mentor.getInstance().getId())
                    .addUserDetails()
                    .push()
                val data = mapOf("is_clicked" to "true")
//                AppObjectController.chatNetworkService.engageNotificationAsync(notificationId, data)
                Timber.d("notifEngage12 : ( $notificationId , clicked)")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun seenNotificationAndDismissed(notificationId: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (notificationId.isNullOrEmpty()) {
                    return@launch
                }
                AppAnalytics
                    .create(AnalyticsEvent.NOTIFICATION_SEEN.NAME)
                    .addParam("id", notificationId)
//                    .addParam("mentorId", Mentor.getInstance().getId())
                    .addUserDetails()
                    .push()
                val data = mapOf("is_clicked" to "false")
//                AppObjectController.chatNetworkService.engageNotificationAsync(notificationId, data)
                Timber.d("notifEngage12 : ( $notificationId , dismissed)")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
