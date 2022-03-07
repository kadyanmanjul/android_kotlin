package com.joshtalks.joshskills.ui.voip.notification

/**
 * notificationData contains the information of notification
 * notificationData={CallType(fpp,gp,normal),userDetails
 */
interface NotificationInterface {
    fun addNotification(notificationType: NotificationType, notificationData: NotificationData):Int
    fun removeNotification(notificationId:Int)
    fun getNotificationObject(notificationType: NotificationType, notificationData: NotificationData): NotificationDetails
    fun updateNotification(notificationId: Int, notificationType: NotificationType, notificationData: NotificationData)
}
interface NotificationData{
    fun getCallType(): CallType
    fun getCallDetails():HashMap<String,Any>
}
