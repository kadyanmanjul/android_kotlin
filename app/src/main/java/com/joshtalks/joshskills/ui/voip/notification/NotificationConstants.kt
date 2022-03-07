package com.joshtalks.joshskills.ui.voip.notification

import androidx.core.app.NotificationCompat

const val ACTION_ACCEPT_CALL="ACCEPT_CALL"
const val ACTION_REJECT_CALL="REJECT_CALL"

sealed class NotificationType {
    object Normal: NotificationType()
    object Urgent: NotificationType()
}
sealed class CallType {
    object FavoritePracticePartner: CallType()
    object GroupCall: CallType()
    object NormalPracticePartner: CallType()

}
data class NotificationDetails( val notificationBuilder: NotificationCompat.Builder, val notificationId:Int)





