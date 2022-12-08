package com.joshtalks.joshskills.voip.notification

import android.app.PendingIntent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat


internal interface NotificationInterface {
    fun removeNotification(notificationId:Int? = null)
    fun getNotificationObject(): NotificationCompat.Builder
    fun getNotificationId(): Int
    fun updateTitle(title:String)
    fun updateContent(content:String)
    fun updateUI(remoteView: RemoteViews)
    fun show()

}

interface NotificationData{
    fun setTitle():String
    fun setContent():String
    fun setTapAction(): PendingIntent? {
        return null
    }
    fun setAction1():NotificationActionObj?{
        return null
    }
    fun setAction2():NotificationActionObj?{
        return null
    }

    interface IncomingNotification{
        fun inflateNotification(map: HashMap<String, String>)
        fun removeNotification()
        fun isNotificationVisible():Boolean
    }

}
