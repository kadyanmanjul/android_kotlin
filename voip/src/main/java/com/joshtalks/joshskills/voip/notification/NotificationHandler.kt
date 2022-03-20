package com.joshtalks.joshskills.voip.notification

import android.app.Application
import com.joshtalks.joshskills.voip.Utils


class NotificationHandler : NotificationInterface {
    private val applicationContext= Utils.context?: Application()
    override fun addNotification(
        notificationType: NotificationType,
        notificationData: NotificationData
    ): Int {
        return NotificationGenerator(applicationContext).initiateNotification(
            notificationData,
            notificationType
        )
    }

    override fun removeNotification(notificationId: Int) {
        NotificationGenerator(applicationContext).removeNotification(notificationId)
    }

    override fun getNotificationObject(
        notificationType: NotificationType,
        notificationData: NotificationData
    ): NotificationDetails {
        return NotificationGenerator(applicationContext).getNotificationObject(
            notificationType,
            notificationData
        )
    }

    override fun updateNotification(
        notificationId: Int,
        notificationType: NotificationType,
        notificationData: NotificationData
    ) {
        NotificationGenerator(applicationContext).updateNotification(
            notificationId,
            notificationData,
            notificationType
        )
    }
}

