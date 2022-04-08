package com.joshtalks.joshskills.voip.notification

import android.app.PendingIntent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class VoipNotification : NotificationInterface {

    private lateinit var notificationBuiltObj: NotificationBuiltObj
    private var ifRemoteView: Boolean = false
    private var remoteView: RemoteViews?=null
    private var notificationPriority:NotificationPriority
    private var notificationData:NotificationData?=null
    private val notificationHelper by lazy {
        NotificationGenerator()
    }

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
            notificationBuiltObj = notificationHelper.initiateNotification(
                notificationData = null,
                remoteView = remoteView,
                notificationPriority = notificationPriority
            )
            ifRemoteView = true
        }else{
            notificationBuiltObj = notificationHelper.initiateNotification(
                notificationData = notificationData,
                remoteView = null,
                notificationPriority = notificationPriority
            )
            ifRemoteView = false
        }
    }

    override fun removeNotification() {
        notificationHelper.removeNotification(notificationBuiltObj.id)
    }

    override fun getNotificationObject(): NotificationCompat.Builder {
        return notificationBuiltObj.notificationBuilder
    }

    override fun getNotificationId(): Int {
        return notificationBuiltObj.id
    }

    override fun updateTitle(title: String) {
        if (!ifRemoteView) {
            notificationHelper.updateTitle(title,notificationBuiltObj)
        }
    }

    override fun updateContent(content: String) {
        if (!ifRemoteView) {
            notificationHelper.updateContent(content,notificationBuiltObj)
        }
    }

    fun connected(username : String, onTap: PendingIntent, onNegativeAction: PendingIntent) {
        notificationHelper.connected(username, notificationBuiltObj, onTap, onNegativeAction)
    }

    fun idle() {
        notificationHelper.idle(notificationBuiltObj)
    }


    override fun updateUI(remoteView: RemoteViews) {
     notificationHelper.updateUI(remoteView, notificationBuiltObj)
    }

    override fun show() {
        notificationHelper.show(notificationBuiltObj)
    }
}