package com.joshtalks.joshskills.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.joshtalks.joshskills.core.PrefManager
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val FCM_TOKEN = "fcmToken"
const val FCM_ID = "fcmId"

class FirebaseTokenService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PrefManager.put(FCM_TOKEN, token)
        FCMTokenManager.pushToken()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val nc: NotificationObject = Gson().fromJson<NotificationObject>(
            Gson().toJson(remoteMessage.data),
            NotificationObject::class.java
        )
        sendNotification(nc)
    }

    private fun sendNotification(notificationObject: NotificationObject) {
        CoroutineScope(Dispatchers.IO).launch {
            val style = NotificationCompat.BigTextStyle()
            style.bigText(notificationObject.contentTitle)
            style.setBigContentTitle(notificationObject.contentText)
            style.setSummaryText(notificationObject.contentText)

            var intent = Intent(applicationContext, InboxActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

            }

            /*val obj = notificationObject.actionData?.let {
                AppObjectController.appDatabase.courseDao().chooseRegisterCourseMinimal(it)
            }
            obj?.let {
                 intent = Intent(applicationContext, ConversationActivity::class.java).apply {
                     intent.putExtra(CHAT_ROOM_OBJECT, it)
                     addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                 }
                intent.putExtra(CHAT_ROOM_OBJECT, it)

            }
*/
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

            val NOTIFICATION_CHANNEL_ID = "101111"

            val notificationBuilder = NotificationCompat.Builder(this@FirebaseTokenService, NOTIFICATION_CHANNEL_ID)
                .setTicker(notificationObject.ticker)
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setContentTitle(notificationObject.contentTitle)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentText(notificationObject.contentText)
                .setContentIntent(pendingIntent)
                .setStyle(style)
                .setColor(Color.parseColor("#f36273"))
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationManager.IMPORTANCE_HIGH)

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "NOTIFICATION_CHANNEL_NAME",
                    importance
                )
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.enableVibration(true)
                notificationChannel.vibrationPattern =
                    longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                notificationBuilder.setChannelId(NOTIFICATION_CHANNEL_ID)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            notificationManager.notify(1, notificationBuilder.build())
        }

    }

}
