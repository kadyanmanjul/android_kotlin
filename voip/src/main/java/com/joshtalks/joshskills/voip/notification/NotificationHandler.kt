package com.joshtalks.joshskills.voip.notification

import android.content.Context
import android.widget.RemoteViews

class NotificationHandler(private val applicationContext: Context
) : NotificationInterface {
    override fun addNotification(notificationData: NotificationData): Int {
        return NotificationGenerator(applicationContext).initiateNotification(
            notificationData = notificationData,
            remoteView = null
        )
    }

    override fun addNotification(remoteView: RemoteViews): Int {
        return NotificationGenerator(applicationContext).initiateNotification(
            notificationData = null,
            remoteView = remoteView
        )
    }

    override fun removeNotification(notificationId: Int) {
        NotificationGenerator(applicationContext).removeNotification(notificationId)
    }

    override fun getNotificationObject(
        notificationData: NotificationData
    ): NotificationDetails {
        return NotificationGenerator(applicationContext).getNotificationObject(
            remoteView= null,notificationData=notificationData
        )
    }

    override fun getNotificationObject(
        remoteView: RemoteViews
    ): NotificationDetails {
        return NotificationGenerator(applicationContext).getNotificationObject(
            remoteView= remoteView,notificationData=null
        )
    }

    override fun updateNotification(
        notificationId: Int,
        notificationData: NotificationData,
    ) {
        NotificationGenerator(applicationContext).updateNotification(
            notificationId,
            remoteView= null,notificationData=notificationData
        )
    }
    override fun updateNotification(
        notificationId: Int,
        remoteView: RemoteViews
    ) {
        NotificationGenerator(applicationContext).updateNotification(
            notificationId,
            remoteView= remoteView,notificationData=null
        )
    }
}