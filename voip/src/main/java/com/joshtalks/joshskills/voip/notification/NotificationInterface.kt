package com.joshtalks.joshskills.voip.notification

import android.app.PendingIntent
import android.widget.RemoteViews

internal interface NotificationInterface {
    fun addNotification(notificationData:NotificationData):Int
    fun addNotification(remoteView: RemoteViews):Int
    fun removeNotification(notificationId:Int)
    fun getNotificationObject(notificationData:NotificationData):NotificationDetails
    fun getNotificationObject(remoteView: RemoteViews):NotificationDetails
    fun updateNotification(notificationId: Int,remoteView: RemoteViews)
    fun updateNotification(notificationId: Int,notificationData:NotificationData)
}

interface NotificationData{
    fun setTitle():String
    fun setContent():String
    fun setTapAction(): PendingIntent?{
        return null
    }
    fun setAction1():NotificationActionObj?{
        return null
    }
    fun setAction2():NotificationActionObj?{
        return null
    }
}
