package com.joshtalks.joshskills.voip.notification

import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class VoipNotification : NotificationInterface{

    private lateinit var notificationBuiltObj: NotificationBuiltObj
    private var ifRemoteView: Boolean = false
    private var remoteView: RemoteViews?=null
    private var notificationPriority:NotificationPriority
    private var notificationData:NotificationData?=null

    constructor(_remoteView: RemoteViews,_notificationPriority:NotificationPriority){
         remoteView = _remoteView
         notificationPriority = _notificationPriority
         buildNotification()
    }
    constructor(_notificationData: NotificationData,_notificationPriority:NotificationPriority){
        notificationData = _notificationData
        notificationPriority = _notificationPriority
        buildNotification()
    }

    private fun buildNotification() {
        if(remoteView!=null){
            notificationBuiltObj = NotificationGenerator().initiateNotification(
                notificationData = null,
                remoteView = remoteView,
                notificationPriority = notificationPriority
            )
            ifRemoteView = true
        }else{
            notificationBuiltObj = NotificationGenerator().initiateNotification(
                notificationData = notificationData,
                remoteView = null,
                notificationPriority = notificationPriority
            )
            ifRemoteView = false
        }
    }

    override fun removeNotification() {
        NotificationGenerator().removeNotification(notificationBuiltObj.id)
    }

    override fun getNotificationObject(): NotificationCompat.Builder {
        return notificationBuiltObj.notificationBuilder
    }

    override fun getNotificationId(): Int {
        return notificationBuiltObj.id
    }

    override fun updateTitle(title: String) {
        if (!ifRemoteView) {
            NotificationGenerator().updateTitle(title,notificationBuiltObj)
        }
    }

    override fun updateContent(content: String) {
        if (!ifRemoteView) {
            NotificationGenerator().updateContent(content,notificationBuiltObj)
        }
    }

    override fun updateUI(remoteView: RemoteViews) {
     NotificationGenerator().updateUI(remoteView, notificationBuiltObj)
    }

    override fun show() {
        NotificationGenerator().show(notificationBuiltObj)
    }
}